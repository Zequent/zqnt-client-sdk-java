package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.detections.domains.DetectionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StoreDetectionRequest {
    private ConnectorRequestContext context;
    private DetectionDTO detection;
}
