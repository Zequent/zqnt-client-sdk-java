package com.zequent.framework.client.sdk.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

    private String taskId;
    private String name;
    private String flightId;
    private String assetSn;
    private String missionId;
}
