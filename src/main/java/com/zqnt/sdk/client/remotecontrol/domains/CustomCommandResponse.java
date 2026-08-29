package com.zqnt.sdk.client.remotecontrol.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomCommandResponse {
    private boolean success;
    private String tid;
    private String sn;
    private String assetId;
    private String message;
    private String commandType;
    private Map<String, Object> result;
    private RemoteControlResponse.ErrorInfo error;
    private RemoteControlResponse.ProgressInfo progress;
}
