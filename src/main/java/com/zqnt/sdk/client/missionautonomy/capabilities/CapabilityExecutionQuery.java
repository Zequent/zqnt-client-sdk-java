package com.zqnt.sdk.client.missionautonomy.capabilities;

import com.zqnt.utils.execution.proto.CapabilityExecutionStatusProto;

/** Optional filters and pagination for capability executions. */
public record CapabilityExecutionQuery(
        String assetSn,
        String organizationId,
        CapabilityExecutionStatusProto status,
        String packageId,
        String capabilityId,
        String theatreId,
        Integer pageSize,
        String pageToken) {

    public CapabilityExecutionQuery {
        if (pageSize != null && (pageSize < 1 || pageSize > 200)) {
            throw new IllegalArgumentException("pageSize must be between 1 and 200");
        }
    }

    public static CapabilityExecutionQuery firstPage() {
        return new CapabilityExecutionQuery(null, null, null, null, null, null, null, null);
    }
}
