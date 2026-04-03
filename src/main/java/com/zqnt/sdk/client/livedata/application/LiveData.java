package com.zqnt.sdk.client.livedata.application;

import com.zqnt.sdk.client.livedata.domains.ChangeLensRequest;
import com.zqnt.sdk.client.livedata.domains.ChangeZoomRequest;
import com.zqnt.sdk.client.livedata.domains.LiveDataResponse;
import com.zqnt.sdk.client.livedata.domains.LiveDataStartLiveStreamRequest;
import com.zqnt.sdk.client.livedata.domains.LiveDataStopLiveStreamRequest;
import com.zqnt.sdk.client.livedata.domains.StreamHandle;
import com.zqnt.sdk.client.livedata.domains.StreamTelemetryRequest;
import com.zqnt.sdk.client.livedata.domains.StreamTelemetryResponse;
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

    CompletableFuture<LiveDataResponse> startLiveStream(LiveDataStartLiveStreamRequest request);

    CompletableFuture<LiveDataResponse> stopLiveStream(LiveDataStopLiveStreamRequest request);


    CompletableFuture<LiveDataResponse> changeCameraLens(ChangeLensRequest request);

    CompletableFuture<LiveDataResponse> changeCameraZoom(ChangeZoomRequest request);
}
