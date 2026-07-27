package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.asset.domains.AssetDTO;
import com.zqnt.utils.asset.domains.SubAssetDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorResponse {
    private boolean success;
    private String tid;
    private String id;
    private LocalDateTime timestamp;
    private String assetId;
    private String message;
    private AssetDTO asset;
    private SubAssetDTO subAsset;
    private Organization organization;
    private ErrorInfo error;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Organization {
        private String id;
        private String name;
        private String description;
        private List<String> assetIds;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ErrorInfo {
        private String errorCode;
        private String errorMessage;
        private LocalDateTime timestamp;
    }
}
