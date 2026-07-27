package com.zqnt.sdk.client.connector.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PayloadOwner {
    public enum Type { ASSET, SUB_ASSET }

    private Type type;
    private String id;
}
