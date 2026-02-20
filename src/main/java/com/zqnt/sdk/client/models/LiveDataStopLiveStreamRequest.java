package com.zequent.framework.client.sdk.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveDataStopLiveStreamRequest {
    private String sn;
    private String tid;
    private String videoId;
}
