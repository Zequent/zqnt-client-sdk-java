package com.zqnt.sdk.client.connector.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GetTechnicalConfigsRequest {
    private ConnectorRequestContext context;
    private String scope;
    private String scopeTarget;
}
