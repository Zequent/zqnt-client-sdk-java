package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.asset.domains.AssetPayloadDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetPayloadListResponse {
    private boolean success;
    private String tid;
    private List<AssetPayloadDTO> payloads;
    private ConnectorResponse.ErrorInfo error;
}
