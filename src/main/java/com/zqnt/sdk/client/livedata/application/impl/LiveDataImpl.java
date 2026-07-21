package com.zqnt.sdk.client.livedata.application.impl;

import com.zqnt.sdk.client.config.GrpcClientConfig;
import com.zqnt.sdk.client.grpc.GrpcResilience;
import com.zqnt.sdk.client.livedata.application.LiveData;
import com.zqnt.sdk.client.livedata.application.LiveDataMapper;
import com.zqnt.sdk.client.livedata.domains.*;
import com.zqnt.utils.common.proto.CommandResponse;
import com.zqnt.utils.events.proto.NotificationResponse;
import com.zqnt.utils.livedata.proto.LiveDataServiceGrpc;
import com.zqnt.utils.livedata.proto.LiveDataTelemetryResponse;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Internal Live Data streaming client using standard gRPC stubs.
 * NOT exposed as a CDI bean - only accessible via ZequentClient.
 * Includes built-in retry logic, circuit breaker, and reconnection handling.
 * <p>
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
    private final ExecutorService unaryCallbackExecutor;
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

        // Stream data is never queued: if all workers are busy, the item is dropped immediately.
        int coreThreads = Math.clamp(Runtime.getRuntime().availableProcessors(), 2, 8);
        ThreadFactory streamThreadFactory = r -> {
            Thread t = new Thread(r, "livedata-stream-handler");
            t.setDaemon(true);
            return t;
        };
        this.streamExecutor = new ThreadPoolExecutor(
                coreThreads, coreThreads,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                streamThreadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );

        // Unary completions are isolated so stream overload cannot delay or discard commands.
        this.unaryCallbackExecutor = new ThreadPoolExecutor(
                2, 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                r -> {
                    Thread t = new Thread(r, "livedata-unary-callback");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // A small scheduler pool prevents one slow timer from delaying every stream and unary timeout.
        int schedulerThreads = Math.clamp(config.getLiveDataSchedulerThreads(), 2, 4);
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(schedulerThreads, r -> {
            Thread t = new Thread(r, "livedata-timeout-scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduler.setRemoveOnCancelPolicy(true);
        this.timeoutScheduler = scheduler;

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

    /** Starts a long-lived telemetry stream which recovers inside the SDK until stopped. */
    @Override
    public StreamHandle streamTelemetryData(StreamTelemetryRequest request,
                                            Consumer<StreamTelemetryResponse> onData,
                                            Consumer<Throwable> onError) {
        StreamHandle handle = new StreamHandle();
        var protoRequest = liveDataMapper.toProtoRequest(request);

        // STOP is a one-shot server command, not a long-lived subscription.
        if (request.getCommand() == com.zqnt.utils.common.proto.LiveDataServiceCommand.LIVE_DATA_COMMAND_STOP_TELEMETRY_STREAM) {
            handle.stop();
            try {
                asyncStub.streamTelemetry(protoRequest, new StreamObserver<>() {
                    @Override public void onNext(LiveDataTelemetryResponse ignored) { }
                    @Override public void onCompleted() { log.debug("Stop telemetry stream acknowledged for sn='{}'", request.getSn()); }
                    @Override public void onError(Throwable error) { notifyError(onError, error); }
                });
            } catch (RuntimeException error) {
                notifyError(onError, error);
            }
            return handle;
        }

        startStream("telemetry", protoRequest, onData, onError, handle, new StreamSubscriptionState(), 0,
                config.getTelemetryHeartbeatTimeoutSeconds(),
                asyncStub::streamTelemetry, liveDataMapper::fromProtoResponse);
        return handle;
    }

    @Override
    public StreamHandle streamNotifications(StreamNotificationRequest request,
                                            Consumer<StreamNotificationResponse> onData,
                                            Consumer<Throwable> onError) {
        StreamHandle handle = new StreamHandle();
        var protoRequest = liveDataMapper.toProtoStreamNotificationsRequest(request);
        startStream("notification", protoRequest, onData, onError, handle, new StreamSubscriptionState(), 0,
                config.getStreamInactivityTimeoutSeconds(),
                asyncStub::streamNotifications, liveDataMapper::fromProtoNotificationResponse);
        return handle;
    }

    /** Convenience overload — terminal/consumer errors are logged automatically. */
    @Override
    public StreamHandle streamTelemetryData(StreamTelemetryRequest request,
                                            Consumer<StreamTelemetryResponse> onData) {
        return streamTelemetryData(request, onData,
                error -> log.error("Unhandled stream error: {}", error.getMessage(), error));
    }

    /**
     * Shared server-streaming lifecycle. Streams deliberately do not use the unary circuit breaker:
     * reconnect/backoff is their recovery mechanism and must not block unrelated commands.
     */
    private <RequestT, ProtoResponseT, ResponseT> void startStream(
            String streamName,
            RequestT request,
            Consumer<ResponseT> onData,
            Consumer<Throwable> onError,
            StreamHandle handle,
            StreamSubscriptionState subscriptionState,
            int reconnectAttempt,
            int configuredInactivityTimeoutSeconds,
            BiConsumer<RequestT, StreamObserver<ProtoResponseT>> streamStarter,
            Function<ProtoResponseT, ResponseT> responseMapper) {
        if (handle.isStopped() || timeoutScheduler.isShutdown()) {
            return;
        }

        subscriptionState.markConnectionStarted();
        int inactivityTimeoutSeconds = Math.max(1, configuredInactivityTimeoutSeconds);
        long inactivityTimeoutNanos = TimeUnit.SECONDS.toNanos(inactivityTimeoutSeconds);
        long checkIntervalMillis = Math.max(1_000L, TimeUnit.SECONDS.toMillis(inactivityTimeoutSeconds) / 6L);
        AtomicBoolean streamEnded = new AtomicBoolean(false);
        AtomicBoolean dataReceived = new AtomicBoolean(false);
        AtomicReference<ScheduledFuture<?>> watchdogRef = new AtomicReference<>();

        Runnable cancelWatchdog = () -> {
            ScheduledFuture<?> watchdog = watchdogRef.getAndSet(null);
            if (watchdog != null) {
                watchdog.cancel(false);
            }
        };

        Runnable reconnect = () -> {
            int nextAttempt = dataReceived.get() ? 1 : Math.min(31, reconnectAttempt + 1);
            scheduleReconnect(streamName, handle, nextAttempt,
                    () -> startStream(streamName, request, onData, onError, handle, subscriptionState, nextAttempt,
                            configuredInactivityTimeoutSeconds, streamStarter, responseMapper));
        };

        ClientResponseObserver<RequestT, ProtoResponseT> observer = new ClientResponseObserver<>() {
            @Override
            public void beforeStart(ClientCallStreamObserver<RequestT> requestStream) {
                handle.bindActiveCall(requestStream);
            }

            @Override
            public void onNext(ProtoResponseT protoResponse) {
                if (streamEnded.get() || handle.isStopped()) {
                    return;
                }
                dataReceived.set(true);
                // Every inbound frame proves that the subscription is alive. In particular,
                // telemetry heartbeats keep an offline/no-data asset from triggering reconnects.
                subscriptionState.markReceived();
                // No store-and-forward: keep at most one in-flight callback per stream.
                if (!subscriptionState.tryBeginCallback()) {
                    return;
                }
                try {
                    streamExecutor.execute(() -> {
                        try {
                            if (handle.isStopped()) {
                                return;
                            }
                            onData.accept(responseMapper.apply(protoResponse));
                        } catch (Exception error) {
                            log.error("Error processing {} stream item: {}", streamName, error.getMessage(), error);
                            notifyError(onError, error);
                        } finally {
                            subscriptionState.endCallback();
                        }
                    });
                } catch (RejectedExecutionException ignored) {
                    // All workers are busy (or shutting down): release this item immediately.
                    subscriptionState.endCallback();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (!streamEnded.compareAndSet(false, true)) {
                    return;
                }
                cancelWatchdog.run();
                if (handle.isStopped()) {
                    return;
                }
                if (!isRetryableStreamError(error)) {
                    log.error("{} stream failed with a non-retryable error", streamName, error);
                    notifyError(onError, error);
                    return;
                }
                log.warn("{} stream disconnected; SDK will reconnect: {}", streamName, error.getMessage());
                reconnect.run();
            }

            @Override
            public void onCompleted() {
                if (!streamEnded.compareAndSet(false, true)) {
                    return;
                }
                cancelWatchdog.run();
                log.info("{} stream completed", streamName);
            }
        };

        ScheduledFuture<?> watchdog;
        try {
            watchdog = timeoutScheduler.scheduleAtFixedRate(() -> {
                if (streamEnded.get() || handle.isStopped()
                        || subscriptionState.nanosSinceLastActivity() < inactivityTimeoutNanos
                        || !streamEnded.compareAndSet(false, true)) {
                    return;
                }
                cancelWatchdog.run();
                handle.cancelActiveCall("client reconnect after inactivity timeout");
                log.warn("{} stream inactive for {}s; SDK will reconnect", streamName, inactivityTimeoutSeconds);
                reconnect.run();
            }, checkIntervalMillis, checkIntervalMillis, MILLISECONDS);
            watchdogRef.set(watchdog);
            handle.bindScheduledTask(watchdog);
        } catch (RejectedExecutionException ignored) {
            return;
        }

        try {
            streamStarter.accept(request, observer);
        } catch (RuntimeException error) {
            if (streamEnded.compareAndSet(false, true)) {
                cancelWatchdog.run();
                if (isRetryableStreamError(error)) {
                    reconnect.run();
                } else {
                    notifyError(onError, error);
                }
            }
        }
    }

    private void scheduleReconnect(String streamName, StreamHandle handle, int attempt, Runnable reconnect) {
        if (handle.isStopped() || timeoutScheduler.isShutdown()) {
            return;
        }
        long delay = reconnectDelayMillis(attempt);
        log.info("Reconnecting {} stream (consecutive attempt {}) in {}ms", streamName, attempt, delay);
        try {
            ScheduledFuture<?> task = timeoutScheduler.schedule(() -> {
                if (!handle.isStopped()) {
                    reconnect.run();
                }
            }, delay, MILLISECONDS);
            handle.bindScheduledTask(task);
        } catch (RejectedExecutionException ignored) {
            // Expected only while the SDK is shutting down.
        }
    }

    long reconnectDelayMillis(int attempt) {
        return reconnectDelayMillis(attempt, config.getRetryDelayMillis(), config.getMaxRetryAttempts());
    }

    static long reconnectDelayMillis(int attempt, long configuredBaseDelay, int configuredMaxExponent) {
        long baseDelay = Math.max(1L, configuredBaseDelay);
        int exponent = Math.min(Math.max(0, attempt - 1), Math.min(30, Math.max(0, configuredMaxExponent)));
        long exponential = baseDelay > (30_000L >> exponent) ? 30_000L : baseDelay << exponent;
        long capped = Math.min(30_000L, exponential);
        long lower = Math.max(1L, Math.round(capped * 0.8d));
        long upper = Math.max(lower, Math.min(30_000L, Math.round(capped * 1.2d)));
        return ThreadLocalRandom.current().nextLong(lower, upper + 1L);
    }

    private boolean isRetryableStreamError(Throwable error) {
        Status.Code code = Status.fromThrowable(error).getCode();
        return switch (code) {
            case CANCELLED, UNKNOWN, DEADLINE_EXCEEDED, RESOURCE_EXHAUSTED,
                    ABORTED, INTERNAL, UNAVAILABLE -> true;
            default -> false;
        };
    }

    private void notifyError(Consumer<Throwable> onError, Throwable error) {
        if (onError == null) {
            return;
        }
        try {
            onError.accept(error);
        } catch (RuntimeException callbackError) {
            log.error("Stream error callback failed", callbackError);
        }
    }

    /** Shared by every connection attempt belonging to one logical subscription. */
    static final class StreamSubscriptionState {
        private final AtomicBoolean processingCallback = new AtomicBoolean(false);
        private final AtomicLong lastActivityNanos = new AtomicLong(System.nanoTime());

        void markReceived() {
            lastActivityNanos.set(System.nanoTime());
        }

        void markConnectionStarted() {
            lastActivityNanos.set(System.nanoTime());
        }

        boolean tryBeginCallback() {
            return processingCallback.compareAndSet(false, true);
        }

        void endCallback() {
            processingCallback.set(false);
        }

        long nanosSinceLastActivity() {
            return System.nanoTime() - lastActivityNanos.get();
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
    public CompletableFuture<CommandResponse> startLiveStream(LiveDataStartLiveStreamRequest request) {
        log.info("Starting live stream for SN: {}, videoId: {}", request.getSn(), request.getVideoId());

        var protoRequest = liveDataMapper.toProtoStartLiveStreamRequest(request);
        int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

        return resilience.executeWithResilienceAsync(() -> {
            CompletableFuture<CommandResponse> future = new CompletableFuture<>();

            // Convert ListenableFuture to CompletableFuture with timeout
            var listenableFuture = futureStub.startLiveStream(protoRequest);

            // Set timeout
            ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
                future.completeExceptionally(new TimeoutException("Start live stream timed out after " + timeout + "s"));
            }, timeout, TimeUnit.SECONDS);

            com.google.common.util.concurrent.Futures.addCallback(listenableFuture,
                    new com.google.common.util.concurrent.FutureCallback<>() {
                        @Override
                        public void onSuccess(CommandResponse result) {
                            timeoutTask.cancel(false);
                            future.complete(result);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            timeoutTask.cancel(false);
                            future.completeExceptionally(t);
                        }
                    },
                    unaryCallbackExecutor
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
    public CompletableFuture<CommandResponse> stopLiveStream(LiveDataStopLiveStreamRequest request) {
        log.info("Stopping live stream for SN: {}, videoId: {}", request.getSn(), request.getVideoId());

        var protoRequest = liveDataMapper.toProtoStopLiveStreamRequest(request);
        int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

        return resilience.executeWithResilienceAsync(() -> {
            CompletableFuture<CommandResponse> future = new CompletableFuture<>();

            var listenableFuture = futureStub.stopLiveStream(protoRequest);

            ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
                future.completeExceptionally(new TimeoutException("Stop live stream timed out after " + timeout + "s"));
            }, timeout, TimeUnit.SECONDS);

            com.google.common.util.concurrent.Futures.addCallback(listenableFuture,
                    new com.google.common.util.concurrent.FutureCallback<>() {
                        @Override
                        public void onSuccess(CommandResponse result) {
                            timeoutTask.cancel(false);
                            future.complete(result);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            timeoutTask.cancel(false);
                            future.completeExceptionally(t);
                        }
                    },
                    unaryCallbackExecutor
            );

            return future;
        });
    }

    @Override
    public CompletableFuture<CommandResponse> changeCameraLens(ChangeLensRequest request) {
        log.info("Changing camera lens for SN: {}", request.getSn());

        var protoRequest = liveDataMapper.toProtoChangeLensRequest(request);
        int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

        return resilience.executeWithResilienceAsync(() -> {
            CompletableFuture<CommandResponse> future = new CompletableFuture<>();

            var listenableFuture = futureStub.changeLens(protoRequest);

            ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
                future.completeExceptionally(new TimeoutException("Change lens timed out after " + timeout + "s"));
            }, timeout, TimeUnit.SECONDS);

            com.google.common.util.concurrent.Futures.addCallback(listenableFuture,
                    new com.google.common.util.concurrent.FutureCallback<>() {
                        @Override
                        public void onSuccess(CommandResponse result) {
                            timeoutTask.cancel(false);
                            future.complete(result);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            timeoutTask.cancel(false);
                            future.completeExceptionally(t);
                        }
                    },
                    unaryCallbackExecutor
            );

            return future;
        });
    }

    @Override
    public CompletableFuture<CommandResponse> changeCameraZoom(ChangeZoomRequest request) {
        log.info("Changing camera zoom for SN: {}", request.getSn());

        var protoRequest = liveDataMapper.toProtoChangeZoomRequest(request);
        int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

        return resilience.executeWithResilienceAsync(() -> {
            CompletableFuture<CommandResponse> future = new CompletableFuture<>();

            var listenableFuture = futureStub.changeZoom(protoRequest);

            ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
                future.completeExceptionally(new TimeoutException("Change zoom timed out after " + timeout + "s"));
            }, timeout, TimeUnit.SECONDS);

            com.google.common.util.concurrent.Futures.addCallback(listenableFuture,
                    new com.google.common.util.concurrent.FutureCallback<>() {
                        @Override
                        public void onSuccess(CommandResponse result) {
                            timeoutTask.cancel(false);
                            future.complete(result);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            timeoutTask.cancel(false);
                            future.completeExceptionally(t);
                        }
                    },
                    unaryCallbackExecutor
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
        unaryCallbackExecutor.shutdown();
        timeoutScheduler.shutdown();
        try {
            if (!streamExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                streamExecutor.shutdownNow();
            }
            if (!unaryCallbackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                unaryCallbackExecutor.shutdownNow();
            }
            if (!timeoutScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            streamExecutor.shutdownNow();
            unaryCallbackExecutor.shutdownNow();
            timeoutScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
