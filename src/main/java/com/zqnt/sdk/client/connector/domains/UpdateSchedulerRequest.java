package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.missionautonomy.domains.SchedulerDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateSchedulerRequest {
    private ConnectorRequestContext context;
    private String schedulerId;
    private SchedulerDTO scheduler;
}
