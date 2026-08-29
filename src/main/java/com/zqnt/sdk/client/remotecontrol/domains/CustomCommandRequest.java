package com.zqnt.sdk.client.remotecontrol.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Executes a logical command advertised by an asset payload. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomCommandRequest {
    private String sn;
    private String tid;
    private String assetId;
    /** Payload external ID or serial number. Optional only when the command is unambiguous. */
    private String componentId;
    /** Explicit target type. Defaults to PAYLOAD when componentId is set. */
    private CapabilityTargetType targetType;
    /** Stable logical command name, for example {@code searchlight.mode.set}. */
    private String commandType;
    private Map<String, Object> params;

    public static CustomCommandRequest forCapability(String sn, CapabilityDescriptor capability,
                                                     Map<String, Object> params) {
        return CustomCommandRequest.builder()
                .sn(sn)
                .commandType(capability.getCommandId())
                .componentId(capability.getTargetRef())
                .targetType(capability.getTargetType())
                .params(params)
                .build();
    }
}
