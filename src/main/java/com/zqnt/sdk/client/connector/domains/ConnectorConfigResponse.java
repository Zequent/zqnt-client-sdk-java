package com.zqnt.sdk.client.connector.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConnectorConfigResponse {
    private boolean success;
    private String tid;
    private LocalDateTime timestamp;
    private List<TechnicalConfig> configs;
    private ConnectorResponse.ErrorInfo error;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TechnicalConfig {
        private String id;
        private String configKey;
        private String configValue;
        private String valueType;
        private String scope;
        private String scopeTarget;
        private boolean active;
        private String description;
    }
}
