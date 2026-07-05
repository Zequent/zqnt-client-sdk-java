package com.zqnt.sdk.client.livedata.application;

import com.zqnt.sdk.client.livedata.domains.*;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface LiveData {

    /**
     * Starts streaming telemetry data with automatic reconnection on failure.
     * Reconnects up to {@code maxRetryAttempts} times with exponential backoff.
     * Use the returned {@link StreamHandle} to stop the stream and cancel reconnection.
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

    StreamHandle streamNotifications(StreamNotificationRequest request, Consumer<StreamNotificationResponse> onData,
                                     Consumer<Throwable> onError);

    CompletableFuture<LiveDataResponse> startLiveStream(LiveDataStartLiveStreamRequest request);

    CompletableFuture<LiveDataResponse> stopLiveStream(LiveDataStopLiveStreamRequest request);


    CompletableFuture<LiveDataResponse> changeCameraLens(ChangeLensRequest request);

    CompletableFuture<LiveDataResponse> changeCameraZoom(ChangeZoomRequest request);
}
