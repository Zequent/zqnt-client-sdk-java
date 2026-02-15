package com.zequent.framework.client.sdk.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualControlRequest {
    private String sn;
    private String assetId;
    private String clientId;
    private String userId;
    private String sessionId;
    private String reason;
}

