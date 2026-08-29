package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.missionautonomy.domains.TaskDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateTaskRequest {
    private ConnectorRequestContext context;
    private TaskDTO task;
}
