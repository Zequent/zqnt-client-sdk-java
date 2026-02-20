package com.zqnt.sdk.client.remotecontrol.application.impl;

import com.zqnt.sdk.client.remotecontrol.application.ManualControlInputSession;
import com.zqnt.sdk.client.remotecontrol.domains.ManualControlInput;
import com.zqnt.sdk.client.remotecontrol.domains.RemoteControlResponse;
import com.zequent.framework.common.proto.RequestBase;
import com.zequent.framework.services.remote.proto.RemoteControlManualControlInputRequest;
import com.zequent.framework.utils.core.ProtobufHelpers;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of ManualControlInputSession for streaming manual control inputs via plain gRPC.
 * Uses StreamObserver for bidirectional streaming (framework-agnostic).
 */
@Slf4j
public class ManualControlInputSessionImpl implements ManualControlInputSession {

    private final StreamObserver<RemoteControlManualControlInputRequest> requestObserver;
    private final CompletableFuture<com.zequent.framework.services.remote.proto.RemoteControlResponse> responseFuture;
    private final String sn;
    private boolean completed = false;

    public ManualControlInputSessionImpl(
            String sn,
            CompletableFuture<com.zequent.framework.services.remote.proto.RemoteControlResponse> responseFuture,
            StreamObserver<RemoteControlManualControlInputRequest> requestObserver) {
        this.sn = sn;
        this.responseFuture = responseFuture;
        this.requestObserver = requestObserver;
    }

    @Override
    public void sendInput(ManualControlInput input) {
        if (completed) {
            throw new IllegalStateException("Session already completed");
        }

        log.debug("Sending manual control input for SN: {}, roll={}, pitch={}, yaw={}, throttle={}, gimbalPitch={}",
                sn, input.getRoll(), input.getPitch(), input.getYaw(), input.getThrottle(), input.getGimbalPitch());

        var builder = com.zequent.framework.common.proto.ManualControlInput.newBuilder();

        if (input.getRoll() != null) {
            builder.setRoll(input.getRoll());
        }
        if (input.getPitch() != null) {
            builder.setPitch(input.getPitch());
        }
        if (input.getYaw() != null) {
            builder.setYaw(input.getYaw());
        }
        if (input.getThrottle() != null) {
            builder.setThrottle(input.getThrottle());
        }
        if (input.getGimbalPitch() != null) {
            builder.setGimbalPitch(input.getGimbalPitch());
        }

        var protoRequest = RemoteControlManualControlInputRequest.newBuilder()
                .setBase(buildBase())
                .setRequest(builder.build())
                .build();

        requestObserver.onNext(protoRequest);
    }

    @Override
    public RemoteControlResponse complete() {
        if (completed) {
            throw new IllegalStateException("Session already completed");
        }

        log.info("Completing manual control input session for SN: {}", sn);
        completed = true;
        requestObserver.onCompleted();

        try {
            var protoResponse = responseFuture.get(30, TimeUnit.SECONDS);
            return toResponse(protoResponse);
        } catch (Exception e) {
            log.error("Failed to get response from manual control input stream", e);
            throw new RuntimeException("Failed to complete manual control input session", e);
        }
    }

    @Override
    public void completeWithError(Throwable error) {
        if (completed) {
            throw new IllegalStateException("Session already completed");
        }

        log.error("Completing manual control input session with error for SN: {}", sn, error);
        completed = true;
        requestObserver.onError(error);
    }

    @Override
    public void close() {
        if (!completed) {
            log.warn("Closing incomplete manual control input session for SN: {}", sn);
            requestObserver.onCompleted();
            completed = true;
        }
    }

    private RequestBase buildBase() {
        var builder = RequestBase.newBuilder()
                .setSn(sn)
                .setTid(UUID.randomUUID().toString())
                .setTimestamp(ProtobufHelpers.now());


        return builder.build();
    }

    private RemoteControlResponse toResponse(com.zequent.framework.services.remote.proto.RemoteControlResponse proto) {
        return RemoteControlResponse.builder()
                .success(!proto.getHasErrors())
                .sn(sn)
                .tid(proto.getTid())
                .message(proto.hasResponseMessage() ? proto.getResponseMessage() : null)
                .assetId(proto.hasAssetId() ? proto.getAssetId() : null)
                .error(proto.hasError() ? RemoteControlResponse.ErrorInfo.builder()
                        .errorCode(proto.getError().getErrorCode().name())
                        .errorMessage(proto.getError().getErrorMessage())
                        .timestamp(ProtobufHelpers.toLocalDateTime(proto.getError().getTimestamp()))
                        .build() : null)
                .progress(proto.hasProgress() ? RemoteControlResponse.ProgressInfo.builder()
                        .progress(proto.getProgress().getProgress())
                        .state(proto.getProgress().getState())
                        .leftTimeInSeconds(proto.getProgress().getLeftTimeInSeconds())
                        .build() : null)
                .build();
    }
}
