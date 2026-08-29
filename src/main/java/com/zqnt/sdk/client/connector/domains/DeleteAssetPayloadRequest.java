package com.zqnt.sdk.client.connector.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DeleteAssetPayloadRequest {
    private ConnectorRequestContext context;
    private PayloadOwner owner;
    private String payloadId;
}
