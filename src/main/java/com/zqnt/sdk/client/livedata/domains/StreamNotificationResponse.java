package com.zqnt.sdk.client.livedata.domains;

import com.zqnt.utils.common.proto.ErrorCode;
import com.zqnt.utils.livedata.proto.NotificationEventType;
import com.zqnt.utils.common.proto.MissionStatus;
import com.zqnt.utils.common.proto.MissionType;
import com.zqnt.utils.common.proto.TaskStatus;
import com.zqnt.utils.common.proto.TaskTypeProto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamNotificationResponse {

    private String tid;
    private Instant timestamp;
    private boolean hasErrors;
    private String sn;
    private String assetId;
    private NotificationEventType eventType;
    private AssetStatusEvent assetStatus;
    private TaskEvent taskEvent;
    private OperationEvent operationEvent;
    private ErrorInfo error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetStatusEvent {
        private String sn;
        private String assetId;
        private Boolean online;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskEvent {
        private String taskId;
        private TaskTypeProto taskType;
        private TaskStatus status;
        private Float progress;
        private String message;
        private String externalTaskType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationEvent {
        private String operationId;
        private MissionType missionType;
        private MissionStatus status;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        private ErrorCode errorCode;
        private String errorMessage;
        private LocalDateTime timestamp;
    }
}
