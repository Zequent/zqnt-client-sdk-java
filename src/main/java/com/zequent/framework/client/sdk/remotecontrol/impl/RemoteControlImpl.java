package com.zequent.framework.client.sdk.remotecontrol.impl;

import com.zequent.framework.client.sdk.config.GrpcClientConfig;
import com.zequent.framework.client.sdk.models.*;
import com.zequent.framework.client.sdk.remotecontrol.ManualControlInputSession;
import com.zequent.framework.client.sdk.remotecontrol.RemoteControl;
import com.zequent.framework.client.sdk.resilience.GrpcResilience;
import com.zequent.framework.common.proto.Coordinates;
import com.zequent.framework.common.proto.RequestBase;
import com.zequent.framework.services.remote.proto.*;
import com.zequent.framework.utils.core.ProtobufHelpers;
import io.grpc.ManagedChannel;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;


/**
 * Internal implementation of the RemoteControl interface.
 * NOT exposed as a CDI bean - only accessible via ZequentClient.
 * Includes built-in retry logic, circuit breaker, and reconnection handling.
 */
@Slf4j
public class RemoteControlImpl implements RemoteControl {

	private final MutinyRemoteControlServiceGrpc.MutinyRemoteControlServiceStub remoteControlService;
	private final GrpcResilience resilience;
    private final GrpcClientConfig config;

	/**
	 * Private constructor - use create() factory method.
	 */
	private RemoteControlImpl(GrpcClientConfig config, ManagedChannel channel) {
		this.config = config;
        this.resilience = new GrpcResilience(
                config.getMaxRetryAttempts(),
                config.getRetryDelayMillis(),
                config.getCircuitBreakerFailureThreshold(),
                config.getCircuitBreakerWaitDurationMillis()
        );
		this.remoteControlService = MutinyRemoteControlServiceGrpc.newMutinyStub(channel);
		log.debug("RemoteControlImpl created with channel for {}:{}",
				config.getRemoteControlConfig().getHost(),
				config.getRemoteControlConfig().getPort());
	}

	/**
	 * Factory method to create RemoteControl implementation.
	 * Called by ZequentClientProducer.
	 */
	public static RemoteControl create(GrpcClientConfig config, ManagedChannel channel) {
		return new RemoteControlImpl(config, channel);
	}

	@Override
	public CompletableFuture<TakeoffResponse> takeoff(TakeoffRequest request) {
		log.info("Takeoff: sn={}", request.getSn());

		var protoRequest = com.zequent.framework.services.remote.proto.RemoteControlTakeOffRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.setRequest(com.zequent.framework.common.proto.Coordinates.newBuilder()
						.setLatitude(request.getLatitude())
						.setLongitude(request.getLongitude())
						.setAltitude(request.getAltitude())
						.build())
				.build();

		return executeAsync(remoteControlService.takeOff(protoRequest))
				.thenApply(proto -> toTakeoffResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> goTo(GoToRequest request) {
		log.info("GoTo: sn={}", request.getSn());

		var protoRequest = RemoteControlGoToRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.setRequest(Coordinates.newBuilder()
						.setLatitude(request.getLatitude())
						.setLongitude(request.getLongitude())
						.setAltitude(request.getAltitude())
						.build())
				.build();

		return executeAsync(remoteControlService.goTo(protoRequest))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> returnToHome(
			com.zequent.framework.client.sdk.models.ReturnToHomeRequest request) {
		log.info("ReturnToHome: sn={}", request.getSn());

		var rthBuilder = com.zequent.framework.common.proto.ReturnToHomeRequest.newBuilder();
		if (request.getAltitude() != null) {
			rthBuilder.setAltitude(request.getAltitude());
		}

		var protoRequest = RemoteControlReturnToHomeRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.setRequest(rthBuilder.build())
				.build();

		return executeAsync(remoteControlService.returnToHome(protoRequest))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> lookAt(LookAtRequest request) {
		log.info("LookAt: sn={}", request.getSn());

		var protoRequest = RemoteControlLookAtRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.setRequest(Coordinates.newBuilder()
						.setLatitude(request.getLatitude())
						.setLongitude(request.getLongitude())
						.setAltitude(request.getAltitude())
						.build())
				.build();

		return executeAsync(remoteControlService.lookAt(protoRequest))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> enterManualControl(
			com.zequent.framework.client.sdk.models.ManualControlRequest request) {
		log.info("EnterManualControl: sn={}", request.getSn());

		var manualControlBuilder = com.zequent.framework.common.proto.ManualControlRequest.newBuilder()
				.setClientId(request.getClientId())
				.setUserId(request.getUserId())
				.setSessionId(request.getSessionId());

		if (request.getReason() != null) {
			manualControlBuilder.setReason(request.getReason());
		}

		var protoRequest = RemoteControlManualControlRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.setRequest(manualControlBuilder.build())
				.build();

		return executeAsync(remoteControlService.enterManualControl(protoRequest))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> exitManualControl(
			com.zequent.framework.client.sdk.models.ManualControlRequest request) {
		log.info("ExitManualControl: sn={}", request.getSn());

		var manualControlBuilder = com.zequent.framework.common.proto.ManualControlRequest.newBuilder()
				.setClientId(request.getClientId())
				.setUserId(request.getUserId())
				.setSessionId(request.getSessionId());

		if (request.getReason() != null) {
			manualControlBuilder.setReason(request.getReason());
		}

		var protoRequest = RemoteControlManualControlRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.setRequest(manualControlBuilder.build())
				.build();

		return executeAsync(remoteControlService.exitManualControl(protoRequest))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public ManualControlInputSession startManualControlInput(String sn, String assetId) {
		log.info("Starting manual control input session for SN: {}", sn);

		// Create a processor to emit stream items
		var processor =
				BroadcastProcessor.<RemoteControlManualControlInputRequest>
						create();

		// Create the gRPC stream call
		var multi = Multi.createFrom().publisher(processor);
		var responseUni = remoteControlService.manualControlInput(multi);

		// Convert to CompletableFuture for the session
		var responseFuture = new CompletableFuture<com.zequent.framework.services.remote.proto.RemoteControlResponse>();

		responseUni.subscribe().with(
				responseFuture::complete,
				responseFuture::completeExceptionally
		);

		return new ManualControlInputSessionImpl(sn, responseFuture, processor);
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> openCover(DockOperationRequest request) {
		log.info("OpenCover: sn={}", request.getSn());

		var protoRequest = RemoteControlOpenCoverRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.build();

		return executeAsync(remoteControlService.openCover(protoRequest))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> closeCover(DockOperationRequest request) {
		log.info("CloseCover: sn={}, force={}", request.getSn(), request.getValue());

		var builder = RemoteControlCloseCoverRequest.newBuilder()
				.setBase(buildBase(request.getSn()));

		if (request.getValue() != null) {
			builder.setForce(request.getValue());
		}

		return executeAsync(remoteControlService.closeCover(builder.build()))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> startCharging(DockOperationRequest request) {
		log.info("StartCharging: sn={}", request.getSn());

		var protoRequest = RemoteControlStartChargingRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.build();

		return executeAsync(remoteControlService.startCharging(protoRequest))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> stopCharging(DockOperationRequest request) {
		log.info("StopCharging: sn={}", request.getSn());

		var protoRequest = RemoteControlStopChargingRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.build();

		return executeAsync(remoteControlService.stopCharging(protoRequest))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> rebootAsset(DockOperationRequest request) {
		log.info("RebootAsset: sn={}", request.getSn());

		var protoRequest = RemoteControlRebootAssetRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.build();

		return executeAsync(remoteControlService.rebootAsset(protoRequest))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> bootSubAsset(DockOperationRequest request) {
		log.info("BootSubAsset: sn={}, boot={}", request.getSn(), request.getValue());

		var protoRequest = RemoteControlBootSubAssetRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.setBoot(request.getValue() != null && request.getValue())
				.build();

		return executeAsync(remoteControlService.bootSubAsset(protoRequest))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> debugMode(DockOperationRequest request) {
		log.info("DebugMode: sn={}, enabled={}", request.getSn(), request.getValue());

		var protoRequest = RemoteControlDebugModeRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.setEnabled(request.getValue() != null && request.getValue())
				.build();

		return executeAsync(remoteControlService.enterOrCloseRemoteDebugMode(protoRequest))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> changeAcMode(DockOperationRequest request) {
		log.info("ChangeAcMode: sn={}", request.getSn());

		var protoRequest = RemoteControlChangeAcModeRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.build();

		return executeAsync(remoteControlService.changeAcMode(protoRequest))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	private com.zequent.framework.common.proto.RequestBase buildBase(String sn) {
		var builder = RequestBase.newBuilder()
				.setSn(sn)
				.setTid(UUID.randomUUID().toString())
				.setTimestamp(ProtobufHelpers.now());

		return builder.build();
	}

	private CompletableFuture<com.zequent.framework.services.remote.proto.RemoteControlResponse> executeAsync(
			io.smallrye.mutiny.Uni<com.zequent.framework.services.remote.proto.RemoteControlResponse> uni) {
		CompletableFuture<com.zequent.framework.services.remote.proto.RemoteControlResponse> future = new CompletableFuture<>();


		// Apply resilience (retry + circuit breaker) and timeout
		resilience.executeWithResilience(uni)
				.ifNoItem().after(java.time.Duration.ofSeconds(config.getRequestTimeoutSeconds())).fail()
				.subscribe().with(
						future::complete,
						throwable -> {
							log.error("Remote control request failed: {}", throwable.getMessage(), throwable);
							future.completeExceptionally(new RuntimeException("Remote control request failed", throwable));
						}
				);

		return future;
	}

	private TakeoffResponse toTakeoffResponse(com.zequent.framework.services.remote.proto.RemoteControlResponse proto, String sn) {
		return TakeoffResponse.builder()
				.success(!proto.getHasErrors())
				.sn(sn)
				.tid(proto.getTid())
				.message(proto.hasResponseMessage() ? proto.getResponseMessage() : null)
				.assetId(proto.hasAssetId() ? proto.getAssetId() : null)
				.error(proto.hasError() ? TakeoffResponse.ErrorInfo.builder()
						.errorCode(proto.getError().getErrorCode().name())
						.errorMessage(proto.getError().getErrorMessage())
						.timestamp(ProtobufHelpers.toLocalDateTime(proto.getError().getTimestamp()))
						.build() : null)
				.progress(proto.hasProgress() ? TakeoffResponse.ProgressInfo.builder()
						.progress(proto.getProgress().getProgress())
						.state(proto.getProgress().getState())
						.leftTimeInSeconds(proto.getProgress().getLeftTimeInSeconds())
						.build() : null)
				.build();
	}

	private com.zequent.framework.client.sdk.models.RemoteControlResponse toResponse(
			com.zequent.framework.services.remote.proto.RemoteControlResponse proto, String sn) {
		return com.zequent.framework.client.sdk.models.RemoteControlResponse.builder()
				.success(!proto.getHasErrors())
				.sn(sn)
				.tid(proto.getTid())
				.message(proto.hasResponseMessage() ? proto.getResponseMessage() : null)
				.assetId(proto.hasAssetId() ? proto.getAssetId() : null)
				.error(proto.hasError() ? com.zequent.framework.client.sdk.models.RemoteControlResponse.ErrorInfo.builder()
						.errorCode(proto.getError().getErrorCode().name())
						.errorMessage(proto.getError().getErrorMessage())
						.timestamp(ProtobufHelpers.toLocalDateTime(proto.getError().getTimestamp()))
						.build() : null)
				.progress(proto.hasProgress() ? com.zequent.framework.client.sdk.models.RemoteControlResponse.ProgressInfo.builder()
						.progress(proto.getProgress().getProgress())
						.state(proto.getProgress().getState())
						.leftTimeInSeconds(proto.getProgress().getLeftTimeInSeconds())
						.build() : null)
				.build();
	}
}
