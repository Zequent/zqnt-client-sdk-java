package com.zqnt.sdk.client.connector.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConnectorPolicyResponse {
    private boolean success;
    private String tid;
    private LocalDateTime timestamp;
    private List<Policy> policies;
    private ConnectorResponse.ErrorInfo error;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Policy {
        private String id;
        private String name;
        private String description;
        private String policyType;
        private String scope;
        private String scopeTarget;
        private int priority;
        private boolean active;
        private String strategyType;
        private String conditions;
        private String constraints;
        private String actions;
    }
}
