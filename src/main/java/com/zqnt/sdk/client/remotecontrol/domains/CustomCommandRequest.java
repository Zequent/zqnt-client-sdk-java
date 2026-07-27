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
    /** Stable logical command name, for example {@code searchlight.mode.set}. */
    private String commandType;
    private Map<String, Object> params;
}
