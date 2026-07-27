package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.asset.domains.AssetPayloadDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpsertAssetPayloadRequest {
    private ConnectorRequestContext context;
    private AssetPayloadDTO payload;
    private String subAssetSn;
    private PayloadOwner owner;
    private List<String> updateFields;
}
