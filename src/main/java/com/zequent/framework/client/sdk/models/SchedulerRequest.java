package com.zequent.framework.client.sdk.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerRequest {
    private String schedulerId;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String missionId;
}
