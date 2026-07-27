package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.asset.domains.AssetDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateAssetRequest {
    private ConnectorRequestContext context;
    private String assetId;
    private AssetDTO asset;
    private List<String> updateFields;
}
