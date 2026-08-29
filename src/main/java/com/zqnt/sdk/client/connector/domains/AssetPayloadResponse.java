package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.asset.domains.AssetPayloadDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetPayloadResponse {
    private boolean success;
    private String tid;
    private AssetPayloadDTO payload;
    private ConnectorResponse.ErrorInfo error;
}
