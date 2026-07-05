package com.zqnt.sdk.client.livedata.application;

import com.google.protobuf.Timestamp;
import com.zqnt.sdk.client.livedata.domains.*;
import com.zqnt.sdk.client.livedata.domains.LiveDataResponse;
import com.zqnt.sdk.client.livedata.domains.LiveDataStartLiveStreamRequest;
import com.zqnt.sdk.client.livedata.domains.LiveDataStopLiveStreamRequest;
import com.zqnt.utils.common.proto.*;
import com.zqnt.utils.core.ProtobufHelpers;
import com.zqnt.utils.edge.sdk.domains.AssetTelemetryData;
import com.zqnt.utils.edge.sdk.domains.SubAssetTelemetryData;
import com.zqnt.utils.livedata.proto.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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

        LiveDataServiceCommand command = request.getCommand() != null
                ? request.getCommand()
                : LiveDataServiceCommand.LIVE_DATA_COMMAND_START_TELEMETRY_STREAM;

        var builder = LiveDataStreamTelemetryRequest.newBuilder()
                .setBase(requestBase(request.getSn(), request.getTid(), false, false))
                .setCommand(command);

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

        response.setAssetId(nullable(protoResponse.hasAssetId(), protoResponse.getAssetId()));

        // Map oneof telemetry fields
        switch (protoResponse.getTelemetryCase()) {
            case ASSET_TELEMETRY:
                response.setAssetTelemetry(mapAssetTelemetry(protoResponse.getAssetTelemetry()));
                break;
            case SUB_ASSET_TELEMETRY:
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
     * Maps StreamNotificationRequest POJO to proto LiveDataStreamNotificationsRequest
     */
    public LiveDataStreamNotificationsRequest toProtoStreamNotificationsRequest(StreamNotificationRequest request) {
        if (request == null) {
            return null;
        }

        var builder = LiveDataStreamNotificationsRequest.newBuilder()
                .setBase(requestBase(request.getSn(), request.getTid(), true, true));

        if (request.getEventTypes() != null && !request.getEventTypes().isEmpty()) {
            builder.addAllEventTypes(request.getEventTypes());
        }

        return builder.build();
    }

    /**
     * Maps proto LiveDataNotificationResponse to StreamNotificationResponse POJO
     */
    public StreamNotificationResponse fromProtoNotificationResponse(LiveDataNotificationResponse protoResponse) {
        if (protoResponse == null) {
            return null;
        }

        var response = StreamNotificationResponse.builder()
                .tid(protoResponse.getTid())
                .timestamp(timestampToInstant(protoResponse.getTimestamp()))
                .hasErrors(protoResponse.getHasErrors())
                .sn(protoResponse.getSn())
                .assetId(nullable(protoResponse.hasAssetId(), protoResponse.getAssetId()))
                .build();

        switch (protoResponse.getEventCase()) {
            case ASSET_STATUS -> response.setAssetStatus(mapAssetStatusEvent(protoResponse.getAssetStatus()));
            case TASK_EVENT -> response.setTaskEvent(mapTaskEvent(protoResponse.getTaskEvent()));
            case OPERATION_EVENT -> response.setOperationEvent(mapOperationEvent(protoResponse.getOperationEvent()));
            case ERROR -> response.setError(mapNotificationErrorInfo(protoResponse.getError()));
            case EVENT_NOT_SET -> {
                // No event set
            }
        }

        response.setEventType(resolveNotificationEventType(protoResponse));
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
                .latitude(nullable(proto.hasLatitude(), proto.getLatitude()))
                .longitude(nullable(proto.hasLongitude(), proto.getLongitude()))
                .absoluteAltitude(nullable(proto.hasAbsoluteAltitude(), proto.getAbsoluteAltitude()))
                .relativeAltitude(nullable(proto.hasRelativeAltitude(), proto.getRelativeAltitude()))
                .environmentTemp(nullable(proto.hasEnvironmentTemp(), proto.getEnvironmentTemp()))
                .insideTemp(nullable(proto.hasInsideTemp(), proto.getInsideTemp()))
                .humidity(nullable(proto.hasHumidity(), proto.getHumidity()))
                .mode(nullable(proto.hasMode(), proto.getMode()))
                .rainfall(nullable(proto.hasRainfall(), proto.getRainfall()))
                .subAssetAtHome(nullable(proto.hasSubAssetAtHome(), proto.getSubAssetAtHome()))
                .subAssetCharging(nullable(proto.hasSubAssetCharging(), proto.getSubAssetCharging()))
                .subAssetPercentage(nullable(proto.hasSubAssetPercentage(), proto.getSubAssetPercentage()))
                .heading(nullable(proto.hasHeading(), proto.getHeading()))
                .debugModeOpen(nullable(proto.hasDebugModeOpen(), proto.getDebugModeOpen()))
                .hasActiveManualControlSession(nullable(proto.hasHasActiveManualControlSession(), proto.getHasActiveManualControlSession()))
                .coverState(nullable(proto.hasCoverState(), proto.getCoverState()))
                .workingVoltage(nullable(proto.hasWorkingVoltage(), proto.getWorkingVoltage()))
                .workingCurrent(nullable(proto.hasWorkingCurrent(), proto.getWorkingCurrent()))
                .supplyVoltage(nullable(proto.hasSupplyVoltage(), proto.getSupplyVoltage()))
                .windSpeed(nullable(proto.hasWindSpeed(), proto.getWindSpeed()))
                .positionValid(nullable(proto.hasPositionValid(), proto.getPositionValid()))
                .manualControlState(nullable(proto.hasManualControlState(), proto.getManualControlState()))
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
        if (proto.hasWirelessLink()) {
            data.setWirelessLink(AssetTelemetryData.WirelessLinkInformation.builder()
                    .sdrFreqBand(proto.getWirelessLink().getSdrFreqBand())
                    .sdrLinkState(proto.getWirelessLink().getSdrLinkState())
                    .sdrQuality(proto.getWirelessLink().getSdrQuality())
                    .fourthGenerationFreqBand(proto.getWirelessLink().getFourthGenerationFreqBand())
                    .fourthGenerationGndQuality(proto.getWirelessLink().getFourthGenerationGndQuality())
                    .fourthGenerationLinkState(proto.getWirelessLink().getFourthGenerationLinkState())
                    .fourthGenerationUavQuality(proto.getWirelessLink().getFourthGenerationUavQuality())
                    .linkWorkmode(proto.getWirelessLink().getLinkWorkmode())
                    .dongleNumber(proto.getWirelessLink().getDongleNumber())
                    .fourthGenerationQuality(proto.getWirelessLink().getFourthGenerationQuality())
                    .build());
        }

        if (proto.hasSdrState()) {
            data.setSdrState(AssetTelemetryData.SdrState.builder()
                    .downQuality(proto.getSdrState().getDownQuality())
                    .upQuality(proto.getSdrState().getUpQuality())
                    .frequencyBand(proto.getSdrState().getFrequencyBand())
                    .build());
        }

        if (proto.hasPositionState()) {
            data.setPositionState(com.zqnt.utils.edge.sdk.domains.AssetTelemetryData.PositionState.builder()
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
                .latitude(nullable(proto.hasLatitude(), proto.getLatitude()))
                .longitude(nullable(proto.hasLongitude(), proto.getLongitude()))
                .absoluteAltitude(nullable(proto.hasAbsoluteAltitude(), proto.getAbsoluteAltitude()))
                .relativeAltitude(nullable(proto.hasRelativeAltitude(), proto.getRelativeAltitude()))
                .horizontalSpeed(nullable(proto.hasHorizontalSpeed(), proto.getHorizontalSpeed()))
                .verticalSpeed(nullable(proto.hasVerticalSpeed(), proto.getVerticalSpeed()))
                .windSpeed(nullable(proto.hasWindSpeed(), proto.getWindSpeed()))
                .windDirection(nullable(proto.hasWindDirection(), proto.getWindDirection()))
                .heading(nullable(proto.hasHeading(), proto.getHeading()))
                .gear(nullable(proto.hasGear(), proto.getGear()))
                .heightLimit(nullable(proto.hasHeightLimit(), proto.getHeightLimit()))
                .homeDistance(nullable(proto.hasHomeDistance(), proto.getHomeDistance()))
                .totalMovementDistance(nullable(proto.hasTotalMovementDistance(), proto.getTotalMovementDistance()))
                .totalMovementTime(nullable(proto.hasTotalMovementTime(), proto.getTotalMovementTime()))
                .mode(nullable(proto.hasMode(), proto.getMode()))
                .country(nullable(proto.hasCountry(), proto.getCountry()))
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
                var cameraData = payloadProto.getCameraData();
                payload.setCameraData(SubAssetTelemetryData.CameraData.builder()
                        .currentLens(cameraData.getCurrentLens())
                        .gimbalPitch(cameraData.getGimbalPitch())
                        .gimbalYaw(cameraData.getGimbalYaw())
                        .gimbalRoll(cameraData.getGimbalRoll())
                        .zoomFactor(cameraData.getZoomFactor())
                        .build());
            }

            if (payloadProto.hasRangeFinderData()) {
                var rangeFinderData = payloadProto.getRangeFinderData();
                payload.setRangeFinderData(SubAssetTelemetryData.RangeFinderData.builder()
                        .targetLatitude(rangeFinderData.getTargetLatitude())
                        .targetLongitude(rangeFinderData.getTargetLongitude())
                        .targetDistance(rangeFinderData.getTargetDistance())
                        .targetAltitude(rangeFinderData.getTargetAltitude())
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

    private StreamNotificationResponse.AssetStatusEvent mapAssetStatusEvent(AssetStatusEvent proto) {
        if (proto == null) {
            return null;
        }

        return StreamNotificationResponse.AssetStatusEvent.builder()
                .sn(proto.getSn())
                .assetId(nullable(proto.hasAssetId(), proto.getAssetId()))
                .online(proto.getOnline())
                .message(nullable(proto.hasMessage(), proto.getMessage()))
                .build();
    }

    private StreamNotificationResponse.TaskEvent mapTaskEvent(TaskEvent proto) {
        if (proto == null) {
            return null;
        }

        return StreamNotificationResponse.TaskEvent.builder()
                .taskId(proto.getTaskId())
                .taskType(proto.getTaskType())
                .status(proto.getStatus())
                .progress(nullable(proto.hasProgress(), proto.getProgress()))
                .message(nullable(proto.hasMessage(), proto.getMessage()))
                .externalTaskType(nullable(proto.hasExternalTaskType(), proto.getExternalTaskType()))
                .build();
    }

    private StreamNotificationResponse.OperationEvent mapOperationEvent(OperationEvent proto) {
        if (proto == null) {
            return null;
        }

        return StreamNotificationResponse.OperationEvent.builder()
                .operationId(proto.getOperationId())
                .missionType(proto.getMissionType())
                .status(proto.getStatus())
                .message(nullable(proto.hasMessage(), proto.getMessage()))
                .build();
    }

    private StreamNotificationResponse.ErrorInfo mapNotificationErrorInfo(GlobalErrorMessage proto) {
        if (proto == null) {
            return null;
        }

        return StreamNotificationResponse.ErrorInfo.builder()
                .errorCode(proto.getErrorCode())
                .errorMessage(proto.getErrorMessage())
                .timestamp(timestampToLocalDateTime(proto.getTimestamp()))
                .build();
    }

    private NotificationEventType resolveNotificationEventType(LiveDataNotificationResponse protoResponse) {
        return switch (protoResponse.getEventCase()) {
            case ASSET_STATUS -> NotificationEventType.NOTIFICATION_EVENT_ASSET_STATUS;
            case TASK_EVENT -> NotificationEventType.NOTIFICATION_EVENT_TASK;
            case OPERATION_EVENT -> NotificationEventType.NOTIFICATION_EVENT_OPERATION;
            case ERROR, EVENT_NOT_SET -> null;
        };
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
    public com.zqnt.utils.livedata.proto.LiveDataStartLiveStreamRequest toProtoStartLiveStreamRequest(
            LiveDataStartLiveStreamRequest request) {
        if (request == null) {
            return null;
        }

        var requestBuilder = com.zqnt.utils.livedata.proto.LiveStreamStartRequest.newBuilder()
                .setVideoId(request.getVideoId())
                .setStreamServer(request.getStreamServer())
                .setStreamType(request.getStreamType())
                .setAssetType(request.getAssetType());

        return com.zqnt.utils.livedata.proto.LiveDataStartLiveStreamRequest.newBuilder()
                .setBase(requestBase(request.getSn(), null, true, false))
                .setRequest(requestBuilder.build())
                .build();
    }

    /**
     * Maps LiveDataStopLiveStreamRequest POJO to proto
     */
    public com.zqnt.utils.livedata.proto.LiveDataStopLiveStreamRequest toProtoStopLiveStreamRequest(
            LiveDataStopLiveStreamRequest request) {
        if (request == null) {
            return null;
        }

        var requestBuilder = com.zqnt.utils.livedata.proto.LiveStreamStopRequest.newBuilder()
                .setVideoId(request.getVideoId());

        return com.zqnt.utils.livedata.proto.LiveDataStopLiveStreamRequest.newBuilder()
                .setBase(requestBase(request.getSn(), request.getTid(), true, false))
                .setRequest(requestBuilder.build())
                .build();
    }

    /**
     * Maps proto LiveDataResponse to POJO
     */
    public LiveDataResponse fromProtoLiveDataResponse(
            com.zqnt.utils.livedata.proto.LiveDataResponse protoResponse) {
        if (protoResponse == null) {
            return null;
        }

        return LiveDataResponse.builder()
                .tid(protoResponse.getTid())
                .timestamp(timestampToLocalDateTime(protoResponse.getTimestamp()))
                .hasErrors(protoResponse.getHasErrors())
                .sn(protoResponse.getSn())
                .assetId(nullable(protoResponse.hasAssetId(), protoResponse.getAssetId()))
                .responseMessage(nullable(protoResponse.hasResponseMessage(), protoResponse.getResponseMessage()))
                .build();
    }


    public LiveDataChangeLensRequest toProtoChangeLensRequest(ChangeLensRequest request) {
        return LiveDataChangeLensRequest.newBuilder()
                .setBase(requestBase(request.getSn(), request.getTid(), false, true))
                .setRequest(ChangeCameraLensRequest.newBuilder()
                        .setLens(request.getLens())
                        .build())
                .build();
    }


    public LiveDataChangeZoomRequest toProtoChangeZoomRequest(ChangeZoomRequest request) {
        return LiveDataChangeZoomRequest.newBuilder()
                .setBase(requestBase(request.getSn(), request.getTid(), false, true))
                .setRequest(ChangeCameraZoomRequest.newBuilder()
                        .setLens(request.getLens())
                        .setZoom(request.getZoom())
                        .build())
                .build();
    }

    private static <T> T nullable(boolean present, T value) {
        return present ? value : null;
    }

    private static RequestBase requestBase(String sn, String tid, boolean generateTidIfMissing, boolean includeTimestamp) {
        var builder = RequestBase.newBuilder()
                .setSn(sn)
                .setTid(generateTidIfMissing && tid == null ? UUID.randomUUID().toString() : tid);

        if (includeTimestamp) {
            builder.setTimestamp(ProtobufHelpers.now());
        }

        return builder.build();
    }
}
