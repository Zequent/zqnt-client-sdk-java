package com.zqnt.sdk.client.livedata.domains;


import lombok.Data;

@Data
public class ChangeLensRequest {
    private String sn;
    private String tid;
    private String lens;
}
