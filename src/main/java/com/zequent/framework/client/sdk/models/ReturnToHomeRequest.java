package com.zequent.framework.client.sdk.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnToHomeRequest {
    private String sn;
    private String assetId;
    private Float altitude;
    private String missionId;
    private String taskId;
}

