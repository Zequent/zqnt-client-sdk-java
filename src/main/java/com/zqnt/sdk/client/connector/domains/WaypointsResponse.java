package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.missionautonomy.domains.WaypointDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WaypointsResponse {
    private boolean success;
    private String tid;
    private String taskId;
    private LocalDateTime timestamp;
    private List<WaypointDTO> waypoints;
    private ConnectorResponse.ErrorInfo error;
}
