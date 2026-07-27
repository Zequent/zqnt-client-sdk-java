package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.asset.domains.SubAssetDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateSubAssetRequest {
    private ConnectorRequestContext context;
    private String subAssetId;
    private SubAssetDTO subAsset;
    private List<String> updateFields;
}
