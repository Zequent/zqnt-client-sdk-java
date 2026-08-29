package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.missionautonomy.domains.MissionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateMissionRequest {
    private ConnectorRequestContext context;
    private MissionDTO mission;
}
