package com.zqnt.sdk.client.missionautonomy.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerResponse {
    private boolean success;
    private String tid;
    private String schedulerId;
    private LocalDateTime timestamp;
    private ErrorInfo error;
    private ProgressInfo progress;
    private SchedulerData schedulerData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        private String errorCode;
        private String errorMessage;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressInfo {
        private float progress;
        private String state;
        private float leftTimeInSeconds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchedulerData {
        private String schedulerId;
        private String name;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }
}
