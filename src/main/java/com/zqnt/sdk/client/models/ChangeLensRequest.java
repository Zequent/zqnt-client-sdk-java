package com.zequent.framework.client.sdk.models;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChangeLensRequest {
    private String sn;
    private String tid;
    private String lens;
}
