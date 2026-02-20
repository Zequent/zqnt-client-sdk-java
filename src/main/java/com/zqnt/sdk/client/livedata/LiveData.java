package com.zequent.framework.client.sdk.livedata;

import com.zequent.framework.client.sdk.models.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface LiveData {

    void streamTelemetryData(StreamTelemetryRequest request,
							 Consumer<StreamTelemetryResponse> onData,
							 Consumer<Throwable> onError);

    void streamTelemetryData(StreamTelemetryRequest request,
                             Consumer<StreamTelemetryResponse> onData);

    CompletableFuture<LiveDataResponse> startLiveStream(LiveDataStartLiveStreamRequest request);

    CompletableFuture<LiveDataResponse> stopLiveStream(LiveDataStopLiveStreamRequest request);


    CompletableFuture<LiveDataResponse> changeCameraLens(ChangeLensRequest request);

    CompletableFuture<LiveDataResponse> changeCameraZoom(ChangeZoomRequest request);
}
