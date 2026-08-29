package com.zqnt.sdk.client.livedata.domains;

import com.zqnt.utils.events.proto.NotificationEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamNotificationRequest {

    private String sn;
    private String tid;

    @Builder.Default
    private List<NotificationEventType> eventTypes = new ArrayList<>();
}
