package com.zqnt.sdk.client.remotecontrol.application;

import com.zqnt.sdk.client.remotecontrol.domains.DockOperationRequest;
import com.zqnt.sdk.client.remotecontrol.domains.GoToRequest;
import com.zqnt.sdk.client.remotecontrol.domains.LookAtRequest;
import com.zqnt.sdk.client.remotecontrol.domains.ManualControlRequest;
import com.zqnt.sdk.client.remotecontrol.domains.RemoteControlResponse;
import com.zqnt.sdk.client.remotecontrol.domains.ReturnToHomeRequest;
import com.zqnt.sdk.client.remotecontrol.domains.TakeoffRequest;
import com.zqnt.sdk.client.remotecontrol.domains.TakeoffResponse;

import java.util.concurrent.CompletableFuture;

public interface RemoteControl {


    // Flight ops
    CompletableFuture<TakeoffResponse> takeoff(TakeoffRequest request);
    CompletableFuture<RemoteControlResponse> goTo(GoToRequest request);
    CompletableFuture<RemoteControlResponse> returnToHome(ReturnToHomeRequest request);
    CompletableFuture<RemoteControlResponse> lookAt(LookAtRequest request);

    // Manual Control
    CompletableFuture<RemoteControlResponse> enterManualControl(ManualControlRequest request);
    CompletableFuture<RemoteControlResponse> exitManualControl(ManualControlRequest request);
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
