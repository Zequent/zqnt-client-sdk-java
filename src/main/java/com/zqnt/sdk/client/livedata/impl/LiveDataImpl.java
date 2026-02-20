package com.zequent.framework.client.sdk.livedata.impl;

import com.zequent.framework.client.sdk.config.GrpcClientConfig;
import com.zequent.framework.client.sdk.livedata.LiveData;
import com.zequent.framework.client.sdk.mapper.LiveDataMapper;
import com.zequent.framework.client.sdk.models.*;
import com.zequent.framework.client.sdk.resilience.GrpcResilience;
import com.zequent.framework.services.livedata.proto.LiveDataServiceGrpc;
import com.zequent.framework.services.livedata.proto.LiveDataTelemetryResponse;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
	 * Stream telemetry data with automatic reconnection on failure.
	 * Uses standard gRPC StreamObserver for optimal performance.
	 *
	 * @param request The streaming request (POJO)
	 * @param onData Callback for each data item (POJO)
	 * @param onError Optional callback for errors
	 */
	public void streamTelemetryData(StreamTelemetryRequest request,
									Consumer<StreamTelemetryResponse> onData,
									Consumer<Throwable> onError) {
		var protoRequest = liveDataMapper.toProtoRequest(request);
		int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

		AtomicBoolean completed = new AtomicBoolean(false);

		// Timeout task
		ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
			if (!completed.get()) {
				String msg = "Stream timed out after " + timeout + " seconds";
				log.warn(msg);
				if (onError != null) {
					onError.accept(new TimeoutException(msg));
				}
			}
		}, timeout, TimeUnit.SECONDS);

		StreamObserver<LiveDataTelemetryResponse> observer =
			new StreamObserver<>() {
				@Override
				public void onNext(LiveDataTelemetryResponse protoResponse) {
					try {
						// Reset timeout on each received item
						timeoutTask.cancel(false);

						// Map to POJO and invoke callback
						var pojoResponse = liveDataMapper.fromProtoResponse(protoResponse);
						onData.accept(pojoResponse);

						resilience.recordSuccess();
					} catch (Exception e) {
						log.error("Error processing stream item: {}", e.getMessage(), e);
						if (onError != null) {
							onError.accept(e);
						}
					}
				}

				@Override
				public void onError(Throwable error) {
					completed.set(true);
					timeoutTask.cancel(false);

					log.error("Stream error: {}", error.getMessage(), error);
					resilience.recordFailure(error);

					if (onError != null) {
						onError.accept(error);
					}
				}

				@Override
				public void onCompleted() {
					completed.set(true);
					timeoutTask.cancel(false);
					log.debug("Stream completed successfully");
				}
			};

		// Execute stream call asynchronously
		streamExecutor.execute(() -> {
			try {
				asyncStub.streamTelemetry(protoRequest, observer);
			} catch (Exception e) {
				log.error("Failed to start stream: {}", e.getMessage(), e);
				if (onError != null) {
					onError.accept(e);
				}
			}
		});
	}

	/**
	 * Stream telemetry data (convenience method without error callback).
	 */
	public void streamTelemetryData(StreamTelemetryRequest request,
									Consumer<StreamTelemetryResponse> onData) {
		streamTelemetryData(request, onData, null);
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
					public void onSuccess(com.zequent.framework.services.livedata.proto.LiveDataResponse result) {
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
					public void onSuccess(com.zequent.framework.services.livedata.proto.LiveDataResponse result) {
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
					public void onSuccess(com.zequent.framework.services.livedata.proto.LiveDataResponse result) {
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
					public void onSuccess(com.zequent.framework.services.livedata.proto.LiveDataResponse result) {
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