package com.zqnt.sdk.client.livedata.domains;

import com.zequent.framework.common.proto.AssetTypeEnum;
import com.zequent.framework.common.proto.LiveStreamTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveDataStartLiveStreamRequest {
    private String sn;
    private String tid;
    private String videoId;
    private String streamServer;
    private LiveStreamTypeEnum streamType;
    private AssetTypeEnum assetType;
}
