package com.zqnt.sdk.client.livedata.domains;


import lombok.Data;

@Data
public class ChangeZoomRequest {

    private String sn;
    private String tid;
    private String lens;
    private int zoom;
}
