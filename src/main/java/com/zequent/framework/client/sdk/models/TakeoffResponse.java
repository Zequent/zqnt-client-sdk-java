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
public class TakeoffResponse {
    private boolean success;
    private String message;
    private String tid;
    private String sn;
    private String assetId;
    private ErrorInfo error;
    private ProgressInfo progress;

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
}
