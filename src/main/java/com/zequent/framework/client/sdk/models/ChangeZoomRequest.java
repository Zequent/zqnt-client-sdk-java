package com.zequent.framework.client.sdk.models;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChangeZoomRequest {

    private String sn;
    private String tid;
    private String lens;
    private int zoom;
}
