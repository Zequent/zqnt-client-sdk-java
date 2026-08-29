package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.asset.domains.AssetDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RegisterAssetRequest {
    private ConnectorRequestContext context;
    private AssetDTO asset;
}
