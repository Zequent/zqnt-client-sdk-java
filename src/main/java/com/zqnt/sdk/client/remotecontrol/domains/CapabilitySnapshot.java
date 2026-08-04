package com.zqnt.sdk.client.remotecontrol.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CapabilitySnapshot {
    private String assetSn;
    private String assetType;
    private String revision;
    private String snapshotState;
    private LocalDateTime observedAt;
    private LocalDateTime validUntil;
    private List<CapabilityDescriptor> capabilities;
}
