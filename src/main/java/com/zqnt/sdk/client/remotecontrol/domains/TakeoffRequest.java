package com.zqnt.sdk.client.remotecontrol.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TakeoffRequest {
    private String sn;
    private String assetId;
    private float latitude;
    private float longitude;
    private float altitude;
    private String missionId;
    private String taskId;
}
