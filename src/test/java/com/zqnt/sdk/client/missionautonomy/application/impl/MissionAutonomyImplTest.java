package com.zqnt.sdk.client.missionautonomy.application.impl;

import com.zqnt.utils.mission.proto.MissionProtoDTO;
import com.zqnt.utils.mission.proto.SchedulerProtoDTO;
import com.zqnt.utils.mission.proto.TaskProtoDTO;
import com.zqnt.utils.missionautonomy.domains.DynamicConfigDTO;
import com.zqnt.utils.missionautonomy.domains.MissionDTO;
import com.zqnt.utils.missionautonomy.domains.SchedulerDTO;
import com.zqnt.utils.missionautonomy.domains.TaskDTO;
import com.zqnt.utils.missionautonomy.domains.config.PoiTaskConfig;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionAutonomyImplTest {

    @Test
    void mapsTypeSafeTaskConfigAndExecutionFields() {
        TaskDTO task = new TaskDTO();
        task.setName("Inspect POI");
        task.setConfig(PoiTaskConfig.builder()
                .poiLatitude(47.1)
                .poiLongitude(8.2)
                .poiAltitude(25f)
                .numberOfOrbits(2)
                .build());
        task.setExternalTaskId("external-1");
        task.setExecutionOrder(3);
        task.setDecisionEngineEnabled(true);

        TaskProtoDTO mapped = MissionAutonomyImpl.mapTaskDtoToProto(TaskProtoDTO.newBuilder(), task).build();

        assertTrue(mapped.hasPoiConfig());
        assertEquals(47.1, mapped.getPoiConfig().getPoiLatitude());
        assertEquals(2, mapped.getPoiConfig().getNumberOfOrbits());
        assertEquals("external-1", mapped.getExternalTaskId());
        assertEquals(3, mapped.getExecutionOrder());
        assertTrue(mapped.getDecisionEngineEnabled());
    }

    @Test
    void mapsNewMissionConfigurationAndNestedTasks() {
        TaskDTO task = new TaskDTO();
        task.setName("Nested task");
        MissionDTO mission = new MissionDTO();
        mission.setName("Mission");
        mission.setDescription("Description");
        mission.setExternalId("external-mission");
        mission.setExternalMissionType("vendor-type");
        mission.setMissionConfig(DynamicConfigDTO.builder()
                .templateId("template-1")
                .templateConfig(Map.of("speed", 12.5))
                .build());
        mission.setTasks(java.util.List.of(task));

        MissionProtoDTO mapped = MissionAutonomyImpl
                .mapMissionDtoToProto(MissionProtoDTO.newBuilder(), mission)
                .build();

        assertEquals("external-mission", mapped.getExternalId());
        assertEquals("vendor-type", mapped.getExternalMissionType());
        assertEquals("template-1", mapped.getMissionConfig().getTemplateId());
        assertEquals(12.5, mapped.getMissionConfig().getTemplateConfig().getFieldsOrThrow("speed").getNumberValue());
        assertEquals("Nested task", mapped.getTasks(0).getName());
    }

    @Test
    void mapsSchedulerAuditTimestamps() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 25, 12, 30);
        SchedulerDTO scheduler = new SchedulerDTO();
        scheduler.setName("Daily");
        scheduler.setCronExpression("0 0 * * *");
        scheduler.setCreatedAt(createdAt);
        scheduler.setModifiedAt(createdAt.plusHours(1));

        SchedulerProtoDTO mapped = MissionAutonomyImpl
                .mapSchedulerDtoToProto(SchedulerProtoDTO.newBuilder(), scheduler)
                .build();

        assertTrue(mapped.hasCreatedAt());
        assertTrue(mapped.hasModifiedAt());
    }
}
