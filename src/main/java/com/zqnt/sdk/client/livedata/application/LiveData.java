package com.zqnt.sdk.client.livedata.application;

import com.zqnt.sdk.client.livedata.domains.*;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface LiveData {

    /**
     * Starts streaming telemetry data with automatic reconnection on failure.
     * Temporary transport failures are recovered indefinitely with capped exponential backoff
     * and jitter. Use the returned {@link StreamHandle} to stop the stream and cancel reconnection.
     * The error callback is reserved for non-retryable request/authentication errors and failures
     * raised by the consumer callback. Delivery is best-effort under local overload: stream items
     * are not buffered and are dropped when the previous callback or all stream workers are busy.
     */
    StreamHandle streamTelemetryData(StreamTelemetryRequest request,
                                     Consumer<StreamTelemetryResponse> onData,
                                     Consumer<Throwable> onError);

    /**
     * Starts streaming telemetry data. Errors are logged automatically.
     * Use the returned {@link StreamHandle} to stop the stream.
     */
    StreamHandle streamTelemetryData(StreamTelemetryRequest request,
                                     Consumer<StreamTelemetryResponse> onData);

    /**
     * Starts a long-lived notification stream with the same SDK-managed recovery semantics as
     * {@link #streamTelemetryData(StreamTelemetryRequest, Consumer, Consumer)}.
     */
    StreamHandle streamNotifications(StreamNotificationRequest request,
                                     Consumer<StreamNotificationResponse> onData,
                                     Consumer<Throwable> onError);

    CompletableFuture<LiveDataResponse> startLiveStream(LiveDataStartLiveStreamRequest request);

    CompletableFuture<LiveDataResponse> stopLiveStream(LiveDataStopLiveStreamRequest request);


    CompletableFuture<LiveDataResponse> changeCameraLens(ChangeLensRequest request);

    CompletableFuture<LiveDataResponse> changeCameraZoom(ChangeZoomRequest request);
}
