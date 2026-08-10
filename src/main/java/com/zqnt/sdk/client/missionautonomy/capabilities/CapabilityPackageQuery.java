package com.zqnt.sdk.client.missionautonomy.capabilities;

import com.zqnt.utils.execution.proto.CapabilityScopeProtoDTO;

/** Optional filters and pagination for capability packages. */
public record CapabilityPackageQuery(
        CapabilityScopeProtoDTO scope,
        Boolean enabledOnly,
        Integer pageSize,
        String pageToken) {

    public CapabilityPackageQuery {
        if (pageSize != null && (pageSize < 1 || pageSize > 200)) {
            throw new IllegalArgumentException("pageSize must be between 1 and 200");
        }
    }

    public static CapabilityPackageQuery firstPage() {
        return new CapabilityPackageQuery(null, null, null, null);
    }
}
