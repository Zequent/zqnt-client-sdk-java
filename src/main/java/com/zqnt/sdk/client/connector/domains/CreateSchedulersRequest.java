package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.missionautonomy.domains.SchedulerDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateSchedulersRequest {
    private ConnectorRequestContext context;
    private List<SchedulerDTO> schedulers;
}
