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
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Internal implementation of RemoteControl using standard gRPC AsyncStub.
 * NOT exposed as a CDI bean - only accessible via ZequentClient.
 *
 * Performance optimizations:
 * - Uses AsyncStub (most performant) for ALL operations
 * - StreamObserver for callback-based non-blocking I/O
 * - CompletableFuture for framework-agnostic async operations
 * - Shared timeout scheduler for resource efficiency
 * - Circuit breaker and retry logic via GrpcResilience
 */
@Slf4j
public class RemoteControlImpl implements RemoteControl {

	private final RemoteControlServiceGrpc.RemoteControlServiceStub asyncStub;
	private final GrpcResilience resilience;
	private final GrpcClientConfig config;
	private final ScheduledExecutorService timeoutScheduler;

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
		this.asyncStub = RemoteControlServiceGrpc.newStub(channel);

		// Scheduler for timeout handling (shared across all calls)
		this.timeoutScheduler = Executors.newScheduledThreadPool(1, r -> {
			Thread t = new Thread(r, "remote-control-timeout");
			t.setDaemon(true);
			return t;
		});

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

		return executeAsync(observer -> asyncStub.takeOff(protoRequest, observer))
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

		return executeAsync(observer -> asyncStub.goTo(protoRequest, observer))
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

		return executeAsync(observer -> asyncStub.returnToHome(protoRequest, observer))
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

		return executeAsync(observer -> asyncStub.lookAt(protoRequest, observer))
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

		return executeAsync(observer -> asyncStub.enterManualControl(protoRequest, observer))
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

		return executeAsync(observer -> asyncStub.exitManualControl(protoRequest, observer))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public ManualControlInputSession startManualControlInput(String sn, String assetId) {
		log.info("Starting manual control input session for SN: {}", sn);

		// CompletableFuture to capture the final response
		var responseFuture = new CompletableFuture<com.zequent.framework.services.remote.proto.RemoteControlResponse>();

		// Response observer to handle server responses
		StreamObserver<com.zequent.framework.services.remote.proto.RemoteControlResponse> responseObserver =
			new StreamObserver<>() {
				@Override
				public void onNext(com.zequent.framework.services.remote.proto.RemoteControlResponse response) {
					responseFuture.complete(response);
				}

				@Override
				public void onError(Throwable t) {
					log.error("Manual control input stream error", t);
					responseFuture.completeExceptionally(t);
				}

				@Override
				public void onCompleted() {
					log.debug("Manual control input stream completed");
				}
			};

		// Start bidirectional streaming - returns request observer
		StreamObserver<RemoteControlManualControlInputRequest> requestObserver =
			asyncStub.manualControlInput(responseObserver);

		return new ManualControlInputSessionImpl(sn, responseFuture, requestObserver);
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> openCover(DockOperationRequest request) {
		log.info("OpenCover: sn={}", request.getSn());

		var protoRequest = RemoteControlOpenCoverRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.build();

		return executeAsync(observer -> asyncStub.openCover(protoRequest, observer))
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

		return executeAsync(observer -> asyncStub.closeCover(builder.build(), observer))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> startCharging(DockOperationRequest request) {
		log.info("StartCharging: sn={}", request.getSn());

		var protoRequest = RemoteControlStartChargingRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.build();

		return executeAsync(observer -> asyncStub.startCharging(protoRequest, observer))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> stopCharging(DockOperationRequest request) {
		log.info("StopCharging: sn={}", request.getSn());

		var protoRequest = RemoteControlStopChargingRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.build();

		return executeAsync(observer -> asyncStub.stopCharging(protoRequest, observer))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> rebootAsset(DockOperationRequest request) {
		log.info("RebootAsset: sn={}", request.getSn());

		var protoRequest = RemoteControlRebootAssetRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.build();

		return executeAsync(observer -> asyncStub.rebootAsset(protoRequest, observer))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> bootSubAsset(DockOperationRequest request) {
		log.info("BootSubAsset: sn={}, boot={}", request.getSn(), request.getValue());

		var protoRequest = RemoteControlBootSubAssetRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.setBoot(request.getValue() != null && request.getValue())
				.build();

		return executeAsync(observer -> asyncStub.bootSubAsset(protoRequest, observer))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> debugMode(DockOperationRequest request) {
		log.info("DebugMode: sn={}, enabled={}", request.getSn(), request.getValue());

		var protoRequest = RemoteControlDebugModeRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.setEnabled(request.getValue() != null && request.getValue())
				.build();

		return executeAsync(observer -> asyncStub.enterOrCloseRemoteDebugMode(protoRequest, observer))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	@Override
	public CompletableFuture<com.zequent.framework.client.sdk.models.RemoteControlResponse> changeAcMode(DockOperationRequest request) {
		log.info("ChangeAcMode: sn={}", request.getSn());

		var protoRequest = RemoteControlChangeAcModeRequest.newBuilder()
				.setBase(buildBase(request.getSn()))
				.build();

		return executeAsync(observer -> asyncStub.changeAcMode(protoRequest, observer))
				.thenApply(proto -> toResponse(proto, request.getSn()));
	}

	private com.zequent.framework.common.proto.RequestBase buildBase(String sn) {
		var builder = RequestBase.newBuilder()
				.setSn(sn)
				.setTid(UUID.randomUUID().toString())
				.setTimestamp(ProtobufHelpers.now());

		return builder.build();
	}

	/**
	 * Execute async gRPC call with resilience and timeout using StreamObserver pattern.
	 * AsyncStub is the most performant approach (callback-based, non-blocking).
	 */
	private CompletableFuture<com.zequent.framework.services.remote.proto.RemoteControlResponse> executeAsync(
			java.util.function.Consumer<StreamObserver<com.zequent.framework.services.remote.proto.RemoteControlResponse>> stubCall) {
		int timeout = config.getRequestTimeoutSeconds();

		return resilience.executeWithResilienceAsync(() -> {
			CompletableFuture<com.zequent.framework.services.remote.proto.RemoteControlResponse> future = new CompletableFuture<>();
			AtomicBoolean completed = new AtomicBoolean(false);

			// Set timeout
			ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
				if (completed.compareAndSet(false, true)) {
					future.completeExceptionally(new TimeoutException("Remote control request timed out after " + timeout + "s"));
				}
			}, timeout, TimeUnit.SECONDS);

			// StreamObserver for callback-based async handling
			StreamObserver<com.zequent.framework.services.remote.proto.RemoteControlResponse> observer = new StreamObserver<>() {
				@Override
				public void onNext(com.zequent.framework.services.remote.proto.RemoteControlResponse response) {
					if (completed.compareAndSet(false, true)) {
						timeoutTask.cancel(false);
						future.complete(response);
					}
				}

				@Override
				public void onError(Throwable t) {
					if (completed.compareAndSet(false, true)) {
						timeoutTask.cancel(false);
						log.error("Remote control request failed: {}", t.getMessage(), t);
						future.completeExceptionally(t);
					}
				}

				@Override
				public void onCompleted() {
					// Response handled in onNext
				}
			};

			// Execute the stub call
			stubCall.accept(observer);
			return future;
		});
	}

	/**
	 * Shutdown executors when done.
	 * Should be called when closing the client.
	 */
	public void shutdown() {
		timeoutScheduler.shutdown();
		try {
			if (!timeoutScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				timeoutScheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			timeoutScheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}
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
