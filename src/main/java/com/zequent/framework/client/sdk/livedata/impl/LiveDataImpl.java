package com.zequent.framework.client.sdk.livedata.impl;

import com.zequent.framework.client.sdk.config.GrpcClientConfig;
import com.zequent.framework.client.sdk.livedata.LiveData;
import com.zequent.framework.client.sdk.mapper.LiveDataMapper;
import com.zequent.framework.client.sdk.models.*;
import com.zequent.framework.client.sdk.resilience.GrpcResilience;
import com.zequent.framework.services.livedata.proto.MutinyLiveDataServiceGrpc;
import io.grpc.ManagedChannel;
import io.smallrye.mutiny.subscription.Cancellable;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Internal Live Data streaming client.
 * NOT exposed as a CDI bean - only accessible via ZequentClient.
 * Includes built-in retry logic, circuit breaker, and reconnection handling.
 */
@Slf4j
public class LiveDataImpl implements LiveData {

	private final MutinyLiveDataServiceGrpc.MutinyLiveDataServiceStub liveDataService;
	private final GrpcResilience resilience;
	private final GrpcClientConfig config;
	private final LiveDataMapper liveDataMapper;

	/**
	 * Private constructor - use create() factory method.
	 */
	private LiveDataImpl(GrpcClientConfig config, ManagedChannel channel,
						 LiveDataMapper liveDataMapper) {
		this.config = config;
		this.resilience = new GrpcResilience(
				config.getMaxRetryAttempts(),
				config.getRetryDelayMillis(),
				config.getCircuitBreakerFailureThreshold(),
				config.getCircuitBreakerWaitDurationMillis()
		);
		this.liveDataService = MutinyLiveDataServiceGrpc.newMutinyStub(channel);
		this.liveDataMapper = liveDataMapper;
		log.debug("LiveData created with channel for {}:{}",
				config.getLiveDataConfig().getHost(),
				config.getLiveDataConfig().getPort());
	}

	/**
	 * Factory method to create LiveData implementation.
	 * Called by ZequentClientProducer.
	 */
	public static LiveDataImpl create(GrpcClientConfig config, ManagedChannel channel) {
		return new LiveDataImpl(config, channel, LiveDataMapper.INSTANCE);
	}

	/**
	 * Stream telemetry data with automatic reconnection on failure.
	 *
	 * @param request The streaming request (POJO)
	 * @param onData Callback for each data item (POJO)
	 * @param onError Optional callback for errors
	 */
	public void streamTelemetryData(StreamTelemetryRequest request,
									Consumer<StreamTelemetryResponse> onData,
									Consumer<Throwable> onError) {
		var protoRequest = liveDataMapper.toProtoRequest(request);

		int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

		// This is a real stream (Multi). DO NOT convert it to Uni() or you'll only get one element.
		var stream = liveDataService.streamTelemetry(protoRequest);

		// Apply resilience (retry + circuit breaker) via GrpcResilience for each item
		var resilientStream = stream
				.onItem().transformToUniAndConcatenate(item ->
					resilience.executeWithResilience(io.smallrye.mutiny.Uni.createFrom().item(item))
				)
				.ifNoItem().after(java.time.Duration.ofSeconds(timeout)).fail()
				.onItem().invoke(protoResponse -> {
					var pojoResponse = liveDataMapper.fromProtoResponse(protoResponse);
					onData.accept(pojoResponse);
				})
				.onFailure().invoke(error -> {
					log.error("Stream error: {}", error.getMessage(), error);
					if (onError != null) {
						onError.accept(error);
					}
				});

		// Start subscription (non-blocking)
		Cancellable ignored = resilientStream.subscribe().with(
				ignoredItem -> { /* already handled in onItem().invoke */ },
				failure -> { /* already handled in onFailure().invoke */ }
		);
	}


	/**
	 * Stream telemetry data (convenience method without error callback).
	 */
	public void streamTelemetryData(StreamTelemetryRequest request,
									Consumer<StreamTelemetryResponse> onData) {
		streamTelemetryData(request, onData, null);
	}

	/**
	 * Start live stream for an asset.
	 *
	 * @param request The start live stream request (POJO)
	 * @return CompletableFuture with the response
	 */
	@Override
	public CompletableFuture<LiveDataResponse> startLiveStream(LiveDataStartLiveStreamRequest request) {
		log.info("Starting live stream for SN: {}, videoId: {}", request.getSn(), request.getVideoId());

		var protoRequest = liveDataMapper.toProtoStartLiveStreamRequest(request);

		int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

		return resilience.executeWithResilience(
				liveDataService.startLiveStream(protoRequest)
						.ifNoItem().after(java.time.Duration.ofSeconds(timeout))
						.failWith(() -> new java.util.concurrent.TimeoutException("Start live stream request timed out"))
		)
		.map(liveDataMapper::fromProtoLiveDataResponse)
		.subscribeAsCompletionStage()
		.toCompletableFuture();
	}

	/**
	 * Stop live stream for an asset.
	 *
	 * @param request The stop live stream request (POJO)
	 * @return CompletableFuture with the response
	 */
	@Override
	public CompletableFuture<LiveDataResponse> stopLiveStream(LiveDataStopLiveStreamRequest request) {
		log.info("Stopping live stream for SN: {}, videoId: {}", request.getSn(), request.getVideoId());

		var protoRequest = liveDataMapper.toProtoStopLiveStreamRequest(request);

		int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

		return resilience.executeWithResilience(
				liveDataService.stopLiveStream(protoRequest)
						.ifNoItem().after(java.time.Duration.ofSeconds(timeout))
						.failWith(() -> new java.util.concurrent.TimeoutException("Stop live stream request timed out"))
		)
		.map(liveDataMapper::fromProtoLiveDataResponse)
		.subscribeAsCompletionStage()
		.toCompletableFuture();
	}

	@Override
	public CompletableFuture<LiveDataResponse> changeCameraLens(ChangeLensRequest request) {
		log.info("Changing camera lens for SN: {}", request.getSn());

		var protoRequest = liveDataMapper.toProtoChangeLensRequest(request);

		int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

		return resilience.executeWithResilience(
				liveDataService.changeLens(protoRequest)
						.ifNoItem().after(java.time.Duration.ofSeconds(timeout))
						.failWith(() -> new java.util.concurrent.TimeoutException("Change camera lens request timed out"))
		)
		.map(liveDataMapper::fromProtoLiveDataResponse)
		.subscribeAsCompletionStage()
		.toCompletableFuture();
	}

	@Override
	public CompletableFuture<LiveDataResponse> changeCameraZoom(ChangeZoomRequest request) {
		log.info("Changing camera zoom for SN: {}", request.getSn());

		var protoRequest = liveDataMapper.toProtoChangeZoomRequest(request);

		int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

		return resilience.executeWithResilience(
				liveDataService.changeZoom(protoRequest)
						.ifNoItem().after(java.time.Duration.ofSeconds(timeout))
						.failWith(() -> new java.util.concurrent.TimeoutException("Change camera zoom request timed out"))
		)
		.map(liveDataMapper::fromProtoLiveDataResponse)
		.subscribeAsCompletionStage()
		.toCompletableFuture();
	}
}