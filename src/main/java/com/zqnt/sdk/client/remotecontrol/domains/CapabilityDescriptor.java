package com.zqnt.sdk.client.remotecontrol.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CapabilityDescriptor {
    private String commandId;
    private String displayName;
    private String description;
    private String state;
    private String unavailableReason;
    private CapabilityTargetType targetType;
    private String targetRef;
    private String schemaVersion;
    private Map<String, String> metadata;
    private Map<String, Object> constraints;
    private Map<String, Object> inputSchema;
    private Map<String, Object> outputSchema;
}
