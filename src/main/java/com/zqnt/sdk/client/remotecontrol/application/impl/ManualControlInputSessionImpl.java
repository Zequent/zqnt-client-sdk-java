package com.zqnt.sdk.client.remotecontrol.application.impl;

import com.zqnt.sdk.client.remotecontrol.application.ManualControlInputSession;
import com.zqnt.sdk.client.remotecontrol.domains.ManualControlInput;
import com.zqnt.sdk.client.remotecontrol.domains.RemoteControlResponse;
import com.zqnt.utils.common.proto.RequestBase;
import com.zqnt.utils.core.ProtobufHelpers;
import com.zqnt.utils.remotecontrol.proto.RemoteControlManualControlInputRequest;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of ManualControlInputSession for streaming manual control inputs via plain gRPC.
 * Uses StreamObserver for bidirectional streaming (framework-agnostic).
 */
@Slf4j
public class ManualControlInputSessionImpl implements ManualControlInputSession {

    private final StreamObserver<RemoteControlManualControlInputRequest> requestObserver;
    private final CompletableFuture<com.zqnt.utils.remotecontrol.proto.RemoteControlResponse> responseFuture;
    private final String sn;
    private final int requestTimeoutSeconds;
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private volatile Throwable streamError;

    public ManualControlInputSessionImpl(
            String sn,
            int requestTimeoutSeconds,
            CompletableFuture<com.zqnt.utils.remotecontrol.proto.RemoteControlResponse> responseFuture,
            StreamObserver<RemoteControlManualControlInputRequest> requestObserver) {
        this.sn = sn;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        this.responseFuture = responseFuture;
        this.requestObserver = requestObserver;
        // Track stream errors from server side so sendInput can detect them early
        this.responseFuture.whenComplete((response, error) -> {
            if (error != null) {
                this.streamError = error;
            }
        });
    }

    @Override
    public void sendInput(ManualControlInput input) {
        if (input == null) {
            throw new IllegalArgumentException("ManualControlInput must not be null");
        }
        if (completed.get()) {
            throw new IllegalStateException("Session already completed");
        }
        if (streamError != null) {
            throw new IllegalStateException("Stream has already failed: " + streamError.getMessage(), streamError);
        }

        log.debug("Sending manual control input for SN: {}, roll={}, pitch={}, yaw={}, throttle={}, gimbalPitch={}",
                sn, input.getRoll(), input.getPitch(), input.getYaw(), input.getThrottle(), input.getGimbalPitch());

        var builder = com.zqnt.utils.common.proto.ManualControlInput.newBuilder();

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

        try {
            requestObserver.onNext(protoRequest);
        } catch (Exception e) {
            log.error("Failed to send manual control input for SN: {}", sn, e);
            throw new RuntimeException("Failed to send manual control input", e);
        }
    }

    @Override
    public RemoteControlResponse complete() {
        if (!completed.compareAndSet(false, true)) {
            throw new IllegalStateException("Session already completed");
        }

        log.info("Completing manual control input session for SN: {}", sn);
        try {
            requestObserver.onCompleted();
        } catch (Exception e) {
            log.warn("Error while completing gRPC stream for SN: {}", sn, e);
        }

        try {
            var protoResponse = responseFuture.get(requestTimeoutSeconds, TimeUnit.SECONDS);
            return toResponse(protoResponse);
        } catch (TimeoutException e) {
            log.error("Timed out waiting for response from manual control input stream for SN: {} after {}s",
                    sn, requestTimeoutSeconds);
            throw new RuntimeException("Manual control input session timed out after " + requestTimeoutSeconds + "s", e);
        } catch (Exception e) {
            log.error("Failed to get response from manual control input stream for SN: {}", sn, e);
            throw new RuntimeException("Failed to complete manual control input session", e);
        }
    }

    @Override
    public void completeWithError(Throwable error) {
        if (!completed.compareAndSet(false, true)) {
            log.warn("Attempted to complete-with-error an already completed session for SN: {}", sn);
            return;
        }

        log.error("Completing manual control input session with error for SN: {}", sn, error);
        try {
            requestObserver.onError(error);
        } catch (Exception e) {
            log.warn("Error while sending gRPC stream error for SN: {}", sn, e);
        }
    }

    @Override
    public void close() {
        if (completed.compareAndSet(false, true)) {
            log.warn("Closing incomplete manual control input session for SN: {}", sn);
            try {
                requestObserver.onCompleted();
            } catch (Exception e) {
                log.warn("Error while closing gRPC stream for SN: {}", sn, e);
            }
        }
    }

    private RequestBase buildBase() {
        return RequestBase.newBuilder()
                .setSn(sn)
                .setTid(UUID.randomUUID().toString())
                .setTimestamp(ProtobufHelpers.now())
                .build();
    }

    private RemoteControlResponse toResponse(com.zqnt.utils.remotecontrol.proto.RemoteControlResponse proto) {
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
