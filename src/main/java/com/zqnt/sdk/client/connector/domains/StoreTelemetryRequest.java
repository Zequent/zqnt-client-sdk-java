package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.edge.sdk.domains.TelemetryData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StoreTelemetryRequest {
    public enum SourceType { ASSET, SUB_ASSET }

    private ConnectorRequestContext context;
    private SourceType sourceType;
    private String assetId;
    private String sourceSystem;
    private TelemetryData telemetry;
    private Map<String, String> additionalData;
}
