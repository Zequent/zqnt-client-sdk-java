package com.zqnt.sdk.client.connector.domains;

import com.zqnt.utils.missionautonomy.domains.MissionZoneDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UploadMissionZonesRequest {
    private ConnectorRequestContext context;
    private String missionId;
    private List<MissionZoneDTO> zones;
    private boolean replaceExisting;
}
