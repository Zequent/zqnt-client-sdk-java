package com.zequent.framework.client.sdk.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveDataResponse {
    private String tid;
    private LocalDateTime timestamp;
    private boolean hasErrors;
    private String sn;
    private String assetId;
    private String responseMessage;

    // Oneof detail fields - only one should be set
    private AssetTelemetryDetail assetTelemetry;
    private SubAssetTelemetryDetail subAssetTelemetry;
    private ErrorDetail errorMessage;
    private LiveStreamStartDetail liveStreamStartResponse;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetTelemetryDetail {
        private String id;
        private LocalDateTime timestamp;
        private Float latitude;
        private Float longitude;
        private Float absoluteAltitude;
        private Float relativeAltitude;
        private Float environmentTemp;
        private Float insideTemp;
        private Float humidity;
        private String mode;
        private String rainfall;
        private Float heading;
        private Boolean debugModeOpen;
        private Boolean hasActiveManualControlSession;
        private String coverState;
        private Integer workingVoltage;
        private Integer workingCurrent;
        private Integer supplyVoltage;
        private Float windSpeed;
        private Boolean positionValid;
        private String manualControlState;
        private PositionStateDetail positionState;
        private SubAssetInformationDetail subAssetInformation;
        private Boolean subAssetAtHome;
        private Boolean subAssetCharging;
        private Float subAssetPercentage;
        private NetworkInformationDetail networkInformation;
        private AirConditionerDetail airConditioner;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubAssetTelemetryDetail {
        private String id;
        private LocalDateTime timestamp;
        private Float latitude;
        private Float longitude;
        private Float absoluteAltitude;
        private Float relativeAltitude;
        private Float horizontalSpeed;
        private Float verticalSpeed;
        private Float windSpeed;
        private String windDirection;
        private Float heading;
        private Integer gear;
        private PayloadTelemetryDetail payloadTelemetry;
        private BatteryInformationDetail batteryInformation;
        private Integer heightLimit;
        private Float homeDistance;
        private Double totalMovementDistance;
        private Double totalMovementTime;
        private String mode;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayloadTelemetryDetail {
        private String id;
        private LocalDateTime timestamp;
        private String name;
        private CameraDataDetail cameraData;
        private RangeFinderDataDetail rangeFinderData;
        private SensorDataDetail sensorData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CameraDataDetail {
        private String currentLens;
        private Float gimbalPitch;
        private Float gimbalYaw;
        private Float zoomFactor;
        private Float gimbalRoll;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RangeFinderDataDetail {
        private Float targetLatitude;
        private Float targetLongitude;
        private Float targetDistance;
        private Float targetAltitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensorDataDetail {
        private Float targetTemperature;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatteryInformationDetail {
        private String percentage;
        private Integer remainingTime;
        private String returnToHomePower;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionStateDetail {
        private Integer gpsNumber;
        private Integer rtkNumber;
        private Integer quality;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubAssetInformationDetail {
        private String sn;
        private String model;
        private Boolean paired;
        private Boolean online;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NetworkInformationDetail {
        private String type;
        private Float rate;
        private String quality;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AirConditionerDetail {
        private String state;
        private Integer switchTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private String errorCode;
        private String errorMessage;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveStreamStartDetail {
        private String streamUrl;
        private String videoId;
    }
}
