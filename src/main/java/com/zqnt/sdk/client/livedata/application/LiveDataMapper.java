package com.zqnt.sdk.client.livedata.application;

import com.google.protobuf.Timestamp;
import com.zqnt.sdk.client.livedata.domains.ChangeLensRequest;
import com.zqnt.sdk.client.livedata.domains.ChangeZoomRequest;
import com.zqnt.sdk.client.livedata.domains.LiveDataResponse;
import com.zqnt.sdk.client.livedata.domains.LiveDataStartLiveStreamRequest;
import com.zqnt.sdk.client.livedata.domains.LiveDataStopLiveStreamRequest;
import com.zqnt.sdk.client.livedata.domains.StreamTelemetryRequest;
import com.zqnt.sdk.client.livedata.domains.StreamTelemetryResponse;
import com.zequent.framework.common.proto.*;
import com.zequent.framework.services.livedata.proto.*;
import com.zequent.framework.utils.core.ProtobufHelpers;
import com.zequent.framework.utils.edge.sdk.dto.AssetTelemetryData;
import com.zequent.framework.utils.edge.sdk.dto.SubAssetTelemetryData;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class LiveDataMapper {

    public static final LiveDataMapper INSTANCE = new LiveDataMapper();

    private LiveDataMapper() {

    }

    /**
     * Maps StreamTelemetryRequest POJO to proto LiveDataStreamTelemetryRequest
     */
    public LiveDataStreamTelemetryRequest toProtoRequest(StreamTelemetryRequest request) {
        if (request == null) {
            return null;
        }

        var builder = LiveDataStreamTelemetryRequest.newBuilder()
                .setBase(RequestBase.newBuilder()
                        .setSn(request.getSn())
                        .setTid(request.getTid())
                        .build())
                .setCommand(LiveDataServiceCommand.START_TELEMETRY_STREAM);

        if (request.getFrequencyMs() > 0) {
            builder.setFrequencyMs(request.getFrequencyMs());
        }

        if (request.getDuration() > 0) {
            builder.setDuration(request.getDuration());
        }

        return builder.build();
    }

    /**
     * Maps proto LiveDataTelemetryResponse to StreamTelemetryResponse POJO
     */
    public StreamTelemetryResponse fromProtoResponse(LiveDataTelemetryResponse protoResponse) {
        if (protoResponse == null) {
            return null;
        }

        StreamTelemetryResponse response = new StreamTelemetryResponse();
        response.setTid(protoResponse.getTid());
        response.setTimestamp(timestampToInstant(protoResponse.getTimestamp()));
        response.setHasErrors(protoResponse.getHasErrors());
        response.setSn(protoResponse.getSn());

        if (protoResponse.hasAssetId()) {
            response.setAssetId(protoResponse.getAssetId());
        }

        // Map oneof telemetry fields
        switch (protoResponse.getTelemetryCase()) {
            case ASSETTELEMETRY:
                response.setAssetTelemetry(mapAssetTelemetry(protoResponse.getAssetTelemetry()));
                break;
            case SUBASSETTELEMETRY:
                response.setSubAssetTelemetry(mapSubAssetTelemetry(protoResponse.getSubAssetTelemetry()));
                break;
            case ERROR:
                response.setError(mapErrorInfo(protoResponse.getError()));
                break;
            case TELEMETRY_NOT_SET:
                // No telemetry set
                break;
        }

        return response;
    }

    /**
     * Maps proto AssetTelemetry to AssetTelemetryData POJO
     */
    private AssetTelemetryData mapAssetTelemetry(AssetTelemetry proto) {
        if (proto == null) {
            return null;
        }

        AssetTelemetryData data = AssetTelemetryData.builder()
                .id(proto.getId())
                .timestamp(timestampToLocalDateTime(proto.getTimestamp()))
                .latitude(proto.hasLatitude() ? proto.getLatitude() : null)
                .longitude(proto.hasLongitude() ? proto.getLongitude() : null)
                .absoluteAltitude(proto.hasAbsoluteAltitude() ? proto.getAbsoluteAltitude() : null)
                .relativeAltitude(proto.hasRelativeAltitude() ? proto.getRelativeAltitude() : null)
                .environmentTemp(proto.hasEnvironmentTemp() ? proto.getEnvironmentTemp() : null)
                .insideTemp(proto.hasInsideTemp() ? proto.getInsideTemp() : null)
                .humidity(proto.hasHumidity() ? proto.getHumidity() : null)
                .mode(proto.hasMode() ? proto.getMode() : null)
                .rainfall(proto.hasRainfall() ? proto.getRainfall() : null)
                .subAssetAtHome(proto.hasSubAssetAtHome() ? proto.getSubAssetAtHome() : null)
                .subAssetCharging(proto.hasSubAssetCharging() ? proto.getSubAssetCharging() : null)
                .subAssetPercentage(proto.hasSubAssetPercentage() ? proto.getSubAssetPercentage() : null)
                .heading(proto.hasHeading() ? proto.getHeading() : null)
                .debugModeOpen(proto.hasDebugModeOpen() ? proto.getDebugModeOpen() : null)
                .hasActiveManualControlSession(proto.hasHasActiveManualControlSession() ? proto.getHasActiveManualControlSession() : null)
                .coverState(proto.hasCoverState() ? proto.getCoverState() : null)
                .workingVoltage(proto.hasWorkingVoltage() ? proto.getWorkingVoltage() : null)
                .workingCurrent(proto.hasWorkingCurrent() ? proto.getWorkingCurrent() : null)
                .supplyVoltage(proto.hasSupplyVoltage() ? proto.getSupplyVoltage() : null)
                .windSpeed(proto.hasWindSpeed() ? proto.getWindSpeed() : null)
                .positionValid(proto.hasPositionValid() ? proto.getPositionValid() : null)
                .manualControlState(proto.hasManualControlState() ? proto.getManualControlState() : null)
                .build();

        // Map nested objects
        if (proto.hasSubAssetInformation()) {
            data.setSubAssetInformation(AssetTelemetryData.SubAssetInformation.builder()
                    .sn(proto.getSubAssetInformation().getSn())
                    .model(proto.getSubAssetInformation().getModel())
                    .paired(proto.getSubAssetInformation().getPaired())
                    .online(proto.getSubAssetInformation().getOnline())
                    .build());
        }

        if (proto.hasNetworkInformation()) {
            data.setNetworkInformation(AssetTelemetryData.NetworkInformation.builder()
                    .type(proto.getNetworkInformation().getType())
                    .rate(proto.getNetworkInformation().getRate())
                    .quality(proto.getNetworkInformation().getQuality())
                    .build());
        }

        if (proto.hasAirConditioner()) {
            data.setAirConditioner(AssetTelemetryData.AirConditioner.builder()
                    .state(proto.getAirConditioner().getState())
                    .switchTime(proto.getAirConditioner().getSwitchTime())
                    .build());
        }

        if (proto.hasPositionState()) {
            data.setPositionState(com.zequent.framework.utils.edge.sdk.dto.AssetTelemetryData.PositionState.builder()
                    .gpsNumber(proto.getPositionState().getGpsNumber())
                    .rtkNumber(proto.getPositionState().getRtkNumber())
                    .quality(proto.getPositionState().getQuality())
                    .build());
        }

        return data;
    }

    /**
     * Maps proto SubAssetTelemetry to SubAssetTelemetryData POJO
     */
    private SubAssetTelemetryData mapSubAssetTelemetry(SubAssetTelemetry proto) {
        if (proto == null) {
            return null;
        }

        SubAssetTelemetryData data = SubAssetTelemetryData.builder()
                .id(proto.getId())
                .timestamp(timestampToLocalDateTime(proto.getTimestamp()))
                .latitude(proto.hasLatitude() ? proto.getLatitude() : null)
                .longitude(proto.hasLongitude() ? proto.getLongitude() : null)
                .absoluteAltitude(proto.hasAbsoluteAltitude() ? proto.getAbsoluteAltitude() : null)
                .relativeAltitude(proto.hasRelativeAltitude() ? proto.getRelativeAltitude() : null)
                .horizontalSpeed(proto.hasHorizontalSpeed() ? proto.getHorizontalSpeed() : null)
                .verticalSpeed(proto.hasVerticalSpeed() ? proto.getVerticalSpeed() : null)
                .windSpeed(proto.hasWindSpeed() ? proto.getWindSpeed() : null)
                .windDirection(proto.hasWindDirection() ? proto.getWindDirection() : null)
                .heading(proto.hasHeading() ? proto.getHeading() : null)
                .gear(proto.hasGear() ? proto.getGear() : null)
                .heightLimit(proto.hasHeightLimit() ? proto.getHeightLimit() : null)
                .homeDistance(proto.hasHomeDistance() ? proto.getHomeDistance() : null)
                .totalMovementDistance(proto.hasTotalMovementDistance() ? proto.getTotalMovementDistance() : null)
                .totalMovementTime(proto.hasTotalMovementTime() ? proto.getTotalMovementTime() : null)
                .mode(proto.hasMode() ? proto.getMode() : null)
                .country(proto.hasCountry() ? proto.getCountry() : null)
                .build();

        // Map battery information
        if (proto.hasBatteryInformation()) {
            data.setBatteryInformation(SubAssetTelemetryData.BatteryInformation.builder()
                    .percentage(proto.getBatteryInformation().getPercentage())
                    .remainingTime(proto.getBatteryInformation().getRemainingTime())
                    .returnToHomePower(proto.getBatteryInformation().getReturnToHomePower())
                    .build());
        }

        // Map payload telemetry
        if (proto.hasPayloadTelemetry()) {
            var payloadProto = proto.getPayloadTelemetry();
            var payload = SubAssetTelemetryData.PayloadTelemetry.builder()
                    .id(payloadProto.getId())
                    .timestamp(timestampToLocalDateTime(payloadProto.getTimestamp()))
                    .name(payloadProto.getName())
                    .build();

            if (payloadProto.hasCameraData()) {
                payload.setCameraData(SubAssetTelemetryData.CameraData.builder()
                        .currentLens(payloadProto.getCameraData().getCurrentLens())
                        .gimbalPitch(payloadProto.getCameraData().getGimbalPitch())
                        .gimbalYaw(payloadProto.getCameraData().getGimbalYaw())
                        .gimbalRoll(payloadProto.getCameraData().getGimbalRoll())
                        .zoomFactor(payloadProto.getCameraData().getZoomFactor())
                        .build());
            }

            if (payloadProto.hasRangeFinderData()) {
                payload.setRangeFinderData(SubAssetTelemetryData.RangeFinderData.builder()
                        .targetLatitude(payloadProto.getRangeFinderData().getTargetLatitude())
                        .targetLongitude(payloadProto.getRangeFinderData().getTargetLongitude())
                        .targetDistance(payloadProto.getRangeFinderData().getTargetDistance())
                        .targetAltitude(payloadProto.getRangeFinderData().getTargetAltitude())
                        .build());
            }

            if (payloadProto.hasSensorData()) {
                payload.setSensorData(SubAssetTelemetryData.SensorData.builder()
                        .targetTemperature(payloadProto.getSensorData().getTargetTemperature())
                        .build());
            }

            data.setPayloadTelemetry(payload);
        }

        return data;
    }

    /**
     * Maps proto GlobalErrorMessage to ErrorInfo POJO
     */
    private StreamTelemetryResponse.ErrorInfo mapErrorInfo(GlobalErrorMessage proto) {
        if (proto == null) {
            return null;
        }

        return StreamTelemetryResponse.ErrorInfo.builder()
                .errorCode(proto.getErrorCode())
                .errorMessage(proto.getErrorMessage())
                .timestamp(timestampToLocalDateTime(proto.getTimestamp()))
                .build();
    }

    /**
     * Converts protobuf Timestamp to Java Instant
     */
    private Instant timestampToInstant(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    /**
     * Converts protobuf Timestamp to LocalDateTime
     */
    private LocalDateTime timestampToLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    /**
     * Maps LiveDataStartLiveStreamRequest POJO to proto
     */
    public com.zequent.framework.services.livedata.proto.LiveDataStartLiveStreamRequest toProtoStartLiveStreamRequest(
            LiveDataStartLiveStreamRequest request) {
        if (request == null) {
            return null;
        }

        var requestBuilder = com.zequent.framework.services.livedata.proto.LiveStreamStartRequest.newBuilder()
                .setVideoId(request.getVideoId())
                .setStreamServer(request.getStreamServer())
                .setStreamType(request.getStreamType())
                .setAssetType(request.getAssetType());

        return com.zequent.framework.services.livedata.proto.LiveDataStartLiveStreamRequest.newBuilder()
                .setBase(RequestBase.newBuilder()
                        .setSn(request.getSn())
                        .setTid(UUID.randomUUID().toString())
                        .build())
                .setRequest(requestBuilder.build())
                .build();
    }

    /**
     * Maps LiveDataStopLiveStreamRequest POJO to proto
     */
    public com.zequent.framework.services.livedata.proto.LiveDataStopLiveStreamRequest toProtoStopLiveStreamRequest(
            LiveDataStopLiveStreamRequest request) {
        if (request == null) {
            return null;
        }

        var requestBuilder = com.zequent.framework.services.livedata.proto.LiveStreamStopRequest.newBuilder()
                .setVideoId(request.getVideoId());

        return com.zequent.framework.services.livedata.proto.LiveDataStopLiveStreamRequest.newBuilder()
                .setBase(RequestBase.newBuilder()
                        .setSn(request.getSn())
                        .setTid(request.getTid() != null ? request.getTid() : UUID.randomUUID().toString())
                        .build())
                .setRequest(requestBuilder.build())
                .build();
    }

    /**
     * Maps proto LiveDataResponse to POJO
     */
    public LiveDataResponse fromProtoLiveDataResponse(
            com.zequent.framework.services.livedata.proto.LiveDataResponse protoResponse) {
        if (protoResponse == null) {
            return null;
        }

        return LiveDataResponse.builder()
                .tid(protoResponse.getTid())
                .timestamp(timestampToLocalDateTime(protoResponse.getTimestamp()))
                .hasErrors(protoResponse.getHasErrors())
                .sn(protoResponse.getSn() )
                .assetId(protoResponse.hasAssetId() ? protoResponse.getAssetId() : null)
                .responseMessage(protoResponse.hasResponseMessage() ? protoResponse.getResponseMessage() : null)
                .build();
    }


    public LiveDataChangeLensRequest toProtoChangeLensRequest(ChangeLensRequest request) {
        return LiveDataChangeLensRequest.newBuilder()
                .setBase(RequestBase.newBuilder()
                        .setSn(request.getSn())
                        .setTid(request.getTid())
                        .setTimestamp(ProtobufHelpers.now()))
                .setRequest(ChangeCameraLensRequest.newBuilder()
                        .setLens(request.getLens())
                        .build())
                .build();
    }


    public LiveDataChangeZoomRequest toProtoChangeZoomRequest(ChangeZoomRequest request) {
        return LiveDataChangeZoomRequest.newBuilder()
                .setBase(RequestBase.newBuilder()
                        .setSn(request.getSn())
                        .setTid(request.getTid())
                        .setTimestamp(ProtobufHelpers.now()))
                .setRequest(ChangeCameraZoomRequest.newBuilder()
                        .setLens(request.getLens())
                        .setZoom(request.getZoom())
                        .build())
                .build();
    }
}
