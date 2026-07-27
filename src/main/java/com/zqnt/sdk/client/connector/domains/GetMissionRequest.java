package com.zqnt.sdk.client.connector.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GetMissionRequest {
    private ConnectorRequestContext context;
    private String missionId;
}
