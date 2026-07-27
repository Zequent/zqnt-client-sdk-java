package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.missionautonomy.domains.MissionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateMissionRequest {
    private ConnectorRequestContext context;
    private String missionId;
    private MissionDTO mission;
}
