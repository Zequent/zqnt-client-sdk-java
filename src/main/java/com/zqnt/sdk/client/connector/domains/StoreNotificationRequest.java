package com.zqnt.sdk.client.connector.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StoreNotificationRequest {
    public enum EventType { ASSET_STATUS, TASK, MISSION }
    public enum Severity { INFO, WARN, CRITICAL }
    public enum TaskType {
        TASK_TYPE_UNSPECIFIED,
        TASK_TYPE_DETECT,
        TASK_TYPE_AREA_MAPPING,
        TASK_TYPE_WAYPOINT,
        TASK_TYPE_POI,
        TASK_TYPE_FOLLOW,
        TASK_TYPE_TRACK,
        TASK_TYPE_COUNTER_DRONE,
        TASK_TYPE_TAKE_OFF,
        TASK_TYPE_GO_TO,
        TASK_TYPE_RETURN_TO_HOME,
        TASK_TYPE_ENTER_MANUAL_CONTROL,
        TASK_TYPE_EXIT_MANUAL_CONTROL,
        TASK_TYPE_LOOK_AT,
        TASK_TYPE_TAKE_PHOTO,
        TASK_TYPE_OPEN_COVER,
        TASK_TYPE_CLOSE_COVER,
        TASK_TYPE_START_CHARGING,
        TASK_TYPE_STOP_CHARGING,
        TASK_TYPE_REBOOT_ASSET,
        TASK_TYPE_BOOT_SUB_ASSET,
        TASK_TYPE_REMOTE_DEBUG,
        TASK_TYPE_CHANGE_AC_MODE,
        TASK_TYPE_CUSTOM_COMMAND,
        TASK_TYPE_EXTERNAL
    }
    public enum TaskStatus {
        TASK_UNKNOWN, TASK_DRAFT, TASK_SCHEDULED, TASK_RUNNING, TASK_ERROR,
        TASK_COMPLETED, TASK_PREPARED, TASK_PAUSED
    }
    public enum MissionType {
        MISSION_TYPE_UNSPECIFIED,
        MISSION_TYPE_AREA_SCAN,
        MISSION_TYPE_POINT_INSPECTION,
        MISSION_TYPE_ROUTE_INSPECTION,
        MISSION_TYPE_PERIMETER_PATROL,
        MISSION_TYPE_ROUTE_PATROL,
        MISSION_TYPE_LIVE_OBSERVATION,
        MISSION_TYPE_TARGET_TRACKING,
        MISSION_TYPE_CROWD_MONITORING,
        MISSION_TYPE_THERMAL_SCAN,
        MISSION_TYPE_MULTISPECTRAL_SCAN,
        MISSION_TYPE_3D_MAPPING,
        MISSION_TYPE_PHOTOGRAMMETRY,
        MISSION_TYPE_DAMAGE_ASSESSMENT,
        MISSION_TYPE_SITUATIONAL_ASSESSMENT,
        MISSION_TYPE_SEARCH,
        MISSION_TYPE_PAYLOAD_DELIVERY,
        MISSION_TYPE_COMMUNICATION_RELAY,
        MISSION_TYPE_DATA_COLLECTION,
        MISSION_TYPE_SECURITY_SWEEP,
        MISSION_TYPE_HAZARD_DETECTION,
        MISSION_TYPE_GAS_DETECTION,
        MISSION_TYPE_RADIATION_DETECTION,
        MISSION_TYPE_CUSTOM
    }
    public enum MissionStatus {
        MISSION_STATUS_UNKNOWN, MISSION_STATUS_DRAFT, MISSION_STATUS_ACTIVE,
        MISSION_STATUS_INACTIVE, MISSION_STATUS_ERROR
    }

    private ConnectorRequestContext context;
    private EventType eventType;
    private Severity severity;
    private AssetStatusEvent assetStatus;
    private TaskEvent task;
    private MissionEvent mission;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AssetStatusEvent {
        private String sn;
        private String assetId;
        private Boolean online;
        private String message;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TaskEvent {
        private String taskId;
        private TaskType taskType;
        private TaskStatus status;
        private Float progress;
        private String message;
        private String externalTaskType;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MissionEvent {
        private String missionId;
        private MissionType missionType;
        private MissionStatus status;
        private String message;
    }
}
