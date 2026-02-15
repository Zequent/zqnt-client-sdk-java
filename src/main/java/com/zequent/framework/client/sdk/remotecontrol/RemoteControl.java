package com.zequent.framework.client.sdk.remotecontrol;

import com.zequent.framework.client.sdk.models.*;

import java.util.concurrent.CompletableFuture;

public interface RemoteControl {


    // Flight ops
    CompletableFuture<TakeoffResponse> takeoff(TakeoffRequest request);
    CompletableFuture<RemoteControlResponse> goTo(GoToRequest request);
    CompletableFuture<RemoteControlResponse> returnToHome(ReturnToHomeRequest request);
    CompletableFuture<RemoteControlResponse> lookAt(LookAtRequest request);

    // Manual Control
    CompletableFuture<RemoteControlResponse> enterManualControl(com.zequent.framework.client.sdk.models.ManualControlRequest request);
    CompletableFuture<RemoteControlResponse> exitManualControl(com.zequent.framework.client.sdk.models.ManualControlRequest request);
	ManualControlInputSession startManualControlInput(String sn, String assetId);

    // Dock ops
    CompletableFuture<RemoteControlResponse> openCover(DockOperationRequest request);
    CompletableFuture<RemoteControlResponse> closeCover(DockOperationRequest request);
    CompletableFuture<RemoteControlResponse> startCharging(DockOperationRequest request);
    CompletableFuture<RemoteControlResponse> stopCharging(DockOperationRequest request);

    // Asset ops
    CompletableFuture<RemoteControlResponse> rebootAsset(DockOperationRequest request);
    CompletableFuture<RemoteControlResponse> bootSubAsset(DockOperationRequest request);
    CompletableFuture<RemoteControlResponse> debugMode(DockOperationRequest request);
    CompletableFuture<RemoteControlResponse> changeAcMode(DockOperationRequest request);
}
