package com.zqnt.sdk.client.livedata.application.impl;

import com.zqnt.sdk.client.config.GrpcClientConfig;
import com.zqnt.sdk.client.grpc.GrpcResilience;
import com.zqnt.sdk.client.livedata.application.LiveData;
import com.zqnt.sdk.client.livedata.application.LiveDataMapper;
import com.zqnt.sdk.client.livedata.domains.*;
import com.zqnt.utils.livedata.proto.LiveDataServiceGrpc;
import com.zqnt.utils.livedata.proto.LiveDataTelemetryResponse;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Internal Live Data streaming client using standard gRPC stubs.
 * NOT exposed as a CDI bean - only accessible via ZequentClient.
 * Includes built-in retry logic, circuit breaker, and reconnection handling.
 *
 * Performance optimizations:
 * - Uses standard gRPC stubs (no Mutiny/Quarkus overhead)
 * - CompletableFuture for async unary calls
 * - StreamObserver for efficient server-streaming
 * - Dedicated thread pool for stream handling
 */
@Slf4j
public class LiveDataImpl implements LiveData {

	private final LiveDataServiceGrpc.LiveDataServiceStub asyncStub;
	private final LiveDataServiceGrpc.LiveDataServiceFutureStub futureStub;
	private final GrpcResilience resilience;
	private final GrpcClientConfig config;
	private final LiveDataMapper liveDataMapper;
	private final ExecutorService streamExecutor;
	private final ScheduledExecutorService timeoutScheduler;

	/**
	 * Private constructor - use create() factory method.
	 */
	private LiveDataImpl(GrpcClientConfig config, ManagedChannel channel,
						 LiveDataMapper liveDataMapper) {
		this.config = config;
		this.resilience = new GrpcResilience(
				config.getMaxRetryAttempts(),
				config.getRetryDelayMillis(),
				config.getCircuitBreakerFailureThreshold(),
				config.getCircuitBreakerWaitDurationMillis()
		);
		this.asyncStub = LiveDataServiceGrpc.newStub(channel);
		this.futureStub = LiveDataServiceGrpc.newFutureStub(channel);
		this.liveDataMapper = liveDataMapper;

		// Dedicated thread pool for stream processing
		this.streamExecutor = Executors.newCachedThreadPool(r -> {
			Thread t = new Thread(r, "livedata-stream-handler");
			t.setDaemon(true);
			return t;
		});

		// Scheduler for timeout handling
		this.timeoutScheduler = Executors.newScheduledThreadPool(1, r -> {
			Thread t = new Thread(r, "livedata-timeout-scheduler");
			t.setDaemon(true);
			return t;
		});

		log.debug("LiveData created with channel for {}:{}",
				config.getLiveDataConfig().getHost(),
				config.getLiveDataConfig().getPort());
	}

	/**
	 * Factory method to create LiveData implementation.
	 * Called by ZequentClientProducer.
	 */
	public static LiveDataImpl create(GrpcClientConfig config, ManagedChannel channel) {
		return new LiveDataImpl(config, channel, LiveDataMapper.INSTANCE);
	}

	/**
	 * Starts streaming telemetry data with automatic reconnection on failure.
	 * Reconnects up to {@code maxRetryAttempts} times (from config) with exponential backoff.
	 * If data was received before a disconnect, the attempt counter resets (treats it as a blip).
	 *
	 * @return a {@link StreamHandle} — call {@code stop()} to cancel the stream and reconnection
	 */
	@Override
	public StreamHandle streamTelemetryData(StreamTelemetryRequest request,
											Consumer<StreamTelemetryResponse> onData,
											Consumer<Throwable> onError) {
		StreamHandle handle = new StreamHandle();
		startStream(request, onData, onError, handle, 0);
		return handle;
	}

	/**
	 * Convenience overload — errors are logged automatically.
	 *
	 * @return a {@link StreamHandle} — call {@code stop()} to cancel the stream and reconnection
	 */
	@Override
	public StreamHandle streamTelemetryData(StreamTelemetryRequest request,
											Consumer<StreamTelemetryResponse> onData) {
		return streamTelemetryData(request, onData,
				error -> log.error("Unhandled stream error (use the overload with onError to handle this): {}", error.getMessage(), error));
	}

	private void startStream(StreamTelemetryRequest request,
							 Consumer<StreamTelemetryResponse> onData,
							 Consumer<Throwable> onError,
							 StreamHandle handle,
							 int reconnectAttempt) {
		if (handle.isStopped()) {
			return;
		}

		try {
			resilience.checkCircuitBreaker();
		} catch (RuntimeException e) {
			log.warn("Rejecting stream request - circuit breaker is OPEN: {}", e.getMessage());
			if (onError != null) {
				onError.accept(e);
			}
			return;
		}

		var protoRequest = liveDataMapper.toProtoRequest(request);
		// Inactivity timeout for streaming: 5 minutes by default (unrelated to unary requestTimeoutSeconds)
		int inactivityTimeoutSeconds = 5 * 60;
		int maxAttempts = config != null ? config.getMaxRetryAttempts() : 3;
		long baseDelayMillis = config != null ? config.getRetryDelayMillis() : 1000L;
		long maxDelayMillis = 30_000L;

		AtomicBoolean streamEnded = new AtomicBoolean(false);
		AtomicBoolean dataReceived = new AtomicBoolean(false);
		AtomicReference<ScheduledFuture<?>> timeoutRef = new AtomicReference<>();

		Runnable scheduleTimeout = () -> {
			ScheduledFuture<?> prev = timeoutRef.getAndSet(
				timeoutScheduler.schedule(() -> {
					if (streamEnded.get() || handle.isStopped()) {
						return;
					}
					streamEnded.set(true);
					log.warn("Stream inactive for {}s, reconnecting...", inactivityTimeoutSeconds);

					// Reconnect: if data was received before, treat as blip and reset counter
					int nextAttempt = dataReceived.get() ? 0 : reconnectAttempt + 1;

					if (nextAttempt > maxAttempts) {
						String msg = "Stream inactive for " + inactivityTimeoutSeconds + "s and max reconnect attempts (" + maxAttempts + ") reached";
						log.error(msg);
						if (onError != null) {
							onError.accept(new TimeoutException(msg));
						}
						return;
					}

					long delay = Math.min(baseDelayMillis * (nextAttempt + 1), maxDelayMillis);
					log.warn("Reconnecting after inactivity timeout (attempt {}/{}), delay {}ms",
							nextAttempt, maxAttempts, delay);
					timeoutScheduler.schedule(() -> {
						if (!handle.isStopped()) {
							startStream(request, onData, onError, handle, nextAttempt);
						}
					}, delay, MILLISECONDS);
				}, inactivityTimeoutSeconds, TimeUnit.SECONDS)
			);
			if (prev != null) prev.cancel(false);
		};

		scheduleTimeout.run();

		StreamObserver<LiveDataTelemetryResponse> observer = new StreamObserver<>() {
			@Override
			public void onNext(LiveDataTelemetryResponse protoResponse) {
				// Ignore data from a zombie stream that was replaced after a timeout-triggered reconnect
				if (streamEnded.get()) {
					return;
				}
				dataReceived.set(true);
				scheduleTimeout.run();

				var pojoResponse = liveDataMapper.fromProtoResponse(protoResponse);
				streamExecutor.execute(() -> {
					try {
						onData.accept(pojoResponse);
						resilience.recordSuccess();
					} catch (Exception e) {
						log.error("Error processing stream item: {}", e.getMessage(), e);
						if (onError != null) {
							onError.accept(e);
						}
					}
				});
			}

			@Override
			public void onError(Throwable error) {
				streamEnded.set(true);
				ScheduledFuture<?> t = timeoutRef.get();
				if (t != null) t.cancel(false);

				resilience.recordFailure(error);

				if (handle.isStopped()) {
					return;
				}

				// If data was received, treat disconnect as a blip and reset attempt counter
				int nextAttempt = dataReceived.get() ? 0 : reconnectAttempt + 1;

				if (nextAttempt > maxAttempts) {
					log.error("Stream failed after {} reconnect attempts, giving up: {}", reconnectAttempt, error.getMessage(), error);
					if (onError != null) {
						onError.accept(error);
					}
					return;
				}

				long delay = Math.min(baseDelayMillis * (nextAttempt + 1), maxDelayMillis);
				log.warn("Stream error (attempt {}/{}), reconnecting in {}ms: {}",
						nextAttempt, maxAttempts, delay, error.getMessage());

				timeoutScheduler.schedule(() -> {
					if (!handle.isStopped()) {
						startStream(request, onData, onError, handle, nextAttempt);
					}
				}, delay, MILLISECONDS);
			}

			@Override
			public void onCompleted() {
				streamEnded.set(true);
				ScheduledFuture<?> t = timeoutRef.get();
				if (t != null) t.cancel(false);
				log.debug("Stream completed");
			}
		};

		try {
			asyncStub.streamTelemetry(protoRequest, observer);
		} catch (Exception e) {
			log.error("Failed to start stream: {}", e.getMessage(), e);
			if (onError != null) {
				onError.accept(e);
			}
		}
	}

	/**
	 * Start live stream for an asset.
	 * Uses ListenableFuture from gRPC for optimal performance.
	 *
	 * @param request The start live stream request (POJO)
	 * @return CompletableFuture with the response
	 */
	@Override
	public CompletableFuture<LiveDataResponse> startLiveStream(LiveDataStartLiveStreamRequest request) {
		log.info("Starting live stream for SN: {}, videoId: {}", request.getSn(), request.getVideoId());

		var protoRequest = liveDataMapper.toProtoStartLiveStreamRequest(request);
		int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

		return resilience.executeWithResilienceAsync(() -> {
			CompletableFuture<LiveDataResponse> future = new CompletableFuture<>();

			// Convert ListenableFuture to CompletableFuture with timeout
			var listenableFuture = futureStub.startLiveStream(protoRequest);

			// Set timeout
			ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
				future.completeExceptionally(new TimeoutException("Start live stream timed out after " + timeout + "s"));
			}, timeout, TimeUnit.SECONDS);

			com.google.common.util.concurrent.Futures.addCallback(listenableFuture,
				new com.google.common.util.concurrent.FutureCallback<>() {
					@Override
					public void onSuccess(com.zqnt.utils.livedata.proto.LiveDataResponse result) {
						timeoutTask.cancel(false);
						future.complete(liveDataMapper.fromProtoLiveDataResponse(result));
					}

					@Override
					public void onFailure(Throwable t) {
						timeoutTask.cancel(false);
						future.completeExceptionally(t);
					}
				},
				streamExecutor
			);

			return future;
		});
	}

	/**
	 * Stop live stream for an asset.
	 *
	 * @param request The stop live stream request (POJO)
	 * @return CompletableFuture with the response
	 */
	@Override
	public CompletableFuture<LiveDataResponse> stopLiveStream(LiveDataStopLiveStreamRequest request) {
		log.info("Stopping live stream for SN: {}, videoId: {}", request.getSn(), request.getVideoId());

		var protoRequest = liveDataMapper.toProtoStopLiveStreamRequest(request);
		int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

		return resilience.executeWithResilienceAsync(() -> {
			CompletableFuture<LiveDataResponse> future = new CompletableFuture<>();

			var listenableFuture = futureStub.stopLiveStream(protoRequest);

			ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
				future.completeExceptionally(new TimeoutException("Stop live stream timed out after " + timeout + "s"));
			}, timeout, TimeUnit.SECONDS);

			com.google.common.util.concurrent.Futures.addCallback(listenableFuture,
				new com.google.common.util.concurrent.FutureCallback<>() {
					@Override
					public void onSuccess(com.zqnt.utils.livedata.proto.LiveDataResponse result) {
						timeoutTask.cancel(false);
						future.complete(liveDataMapper.fromProtoLiveDataResponse(result));
					}

					@Override
					public void onFailure(Throwable t) {
						timeoutTask.cancel(false);
						future.completeExceptionally(t);
					}
				},
				streamExecutor
			);

			return future;
		});
	}

	@Override
	public CompletableFuture<LiveDataResponse> changeCameraLens(ChangeLensRequest request) {
		log.info("Changing camera lens for SN: {}", request.getSn());

		var protoRequest = liveDataMapper.toProtoChangeLensRequest(request);
		int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

		return resilience.executeWithResilienceAsync(() -> {
			CompletableFuture<LiveDataResponse> future = new CompletableFuture<>();

			var listenableFuture = futureStub.changeLens(protoRequest);

			ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
				future.completeExceptionally(new TimeoutException("Change lens timed out after " + timeout + "s"));
			}, timeout, TimeUnit.SECONDS);

			com.google.common.util.concurrent.Futures.addCallback(listenableFuture,
				new com.google.common.util.concurrent.FutureCallback<>() {
					@Override
					public void onSuccess(com.zqnt.utils.livedata.proto.LiveDataResponse result) {
						timeoutTask.cancel(false);
						future.complete(liveDataMapper.fromProtoLiveDataResponse(result));
					}

					@Override
					public void onFailure(Throwable t) {
						timeoutTask.cancel(false);
						future.completeExceptionally(t);
					}
				},
				streamExecutor
			);

			return future;
		});
	}

	@Override
	public CompletableFuture<LiveDataResponse> changeCameraZoom(ChangeZoomRequest request) {
		log.info("Changing camera zoom for SN: {}", request.getSn());

		var protoRequest = liveDataMapper.toProtoChangeZoomRequest(request);
		int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

		return resilience.executeWithResilienceAsync(() -> {
			CompletableFuture<LiveDataResponse> future = new CompletableFuture<>();

			var listenableFuture = futureStub.changeZoom(protoRequest);

			ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
				future.completeExceptionally(new TimeoutException("Change zoom timed out after " + timeout + "s"));
			}, timeout, TimeUnit.SECONDS);

			com.google.common.util.concurrent.Futures.addCallback(listenableFuture,
				new com.google.common.util.concurrent.FutureCallback<>() {
					@Override
					public void onSuccess(com.zqnt.utils.livedata.proto.LiveDataResponse result) {
						timeoutTask.cancel(false);
						future.complete(liveDataMapper.fromProtoLiveDataResponse(result));
					}

					@Override
					public void onFailure(Throwable t) {
						timeoutTask.cancel(false);
						future.completeExceptionally(t);
					}
				},
				streamExecutor
			);

			return future;
		});
	}

	/**
	 * Shutdown executors when done.
	 * Should be called when closing the client.
	 */
	public void shutdown() {
		streamExecutor.shutdown();
		timeoutScheduler.shutdown();
		try {
			if (!streamExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
				streamExecutor.shutdownNow();
			}
			if (!timeoutScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				timeoutScheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			streamExecutor.shutdownNow();
			timeoutScheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}