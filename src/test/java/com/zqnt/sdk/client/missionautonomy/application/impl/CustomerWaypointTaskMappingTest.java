package com.zqnt.sdk.client.missionautonomy.application.impl;

import com.zqnt.utils.mission.proto.*;
import com.zqnt.utils.missionautonomy.domains.TaskDTO;
import com.zqnt.utils.missionautonomy.domains.WaypointDTO;
import com.zqnt.utils.missionautonomy.domains.config.WaypointTaskConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproduces the exact customer TaskDTO reported to end up with an empty persisted config, using
 * the REAL client-sdk mapping entry point ({@link MissionAutonomyImpl#mapTaskDtoToProto}) rather
 * than a hand-mirrored copy of its logic — to rule out that a hand-rolled test accidentally does
 * something "more correct" than the actual production code path.
 */
class CustomerWaypointTaskMappingTest {

    @Test
    void mapsTheExactCustomerTaskDtoWithoutLosingTheWaypointConfig() {
        WaypointTaskConfig config = WaypointTaskConfig.builder()
                .externalTaskId("ad9fcc54-e13c-4dce-b729-01dec12820d4")
                .waypoints(List.of(
                        waypoint(47.776500625, 9.267688194, 40.0f, 0),
                        waypoint(47.776583782, 9.268981112, 40.0f, 1),
                        waypoint(47.77577967, 9.267862663, 40.0f, 2),
                        waypoint(47.775707656, 9.269498303, 40.0f, 3)))
                .flyToWaylineMode(FlyToWaylineModeProto.FTW_MODE_POINT_TO_POINT)
                .waylineFinishAction(WaylineFinishActionProto.WF_ACTION_GO_HOME)
                .waylineType(WaylineTypeEnumProto.WT_WAYPOINT)
                .waylineTurnMode(WaylineTurnModeProto.WT_MODE_TO_POINT_AND_PASS_WITH_CONTINUITY_CURVATURE)
                .useStraightLine(true)
                .waylinePrecisionType(WaylinePrecisionTypeEnumProto.PRECISION_GPS)
                .exitWaylineWhenRcLostEnum(ExitWaylineWhenRcLostEnumProto.EWWRL_EXECUTE_RC_LOST_ACTION)
                .rcLostActionEnum(RcLostActionEnumProto.RC_LOST_ACTION_RETURN_HOME)
                .outOfControlAction(OutOfControlActionEnumProto.OOC_RETURN_TO_HOME)
                .takeOffSecurityHeight(10.0f)
                .rthAltitude(70)
                .rthMode(RthModeEnumProto.RTH_MODE_PRESET)
                .rthSpeed(6.0f)
                .globalSpeed(10.0f)
                .globalTransitionSpeed(8.0f)
                .globalHeight(40.0f)
                .gimbalPitchMode(WaylineGimbalPitchModeProto.WGP_MODE_LOOK_DOWN)
                .globalGimbalPitch(-90)
                .build();

        // Exact field-for-field reconstruction of the customer's TaskDTO.toString() dump.
        TaskDTO taskDTO = TaskDTO.builder()
                .id(null)
                .createdAt(null)
                .modifiedAt(null)
                .modifiedFrom(null)
                .missionId(UUID.fromString("75d334fa-3690-436a-99c7-fdebb4c36547"))
                .name("ererse")
                .description("desc")
                .taskType(TaskTypeProto.TASK_TYPE_WAYPOINT)
                .config(config)
                .externalTaskId(null)
                .taskConfigTemplate(null)
                .autonomyConfig(null)
                .executionOrder(null)
                .decisionEngineEnabled(null)
                .status(TaskStatus.TASK_DRAFT)
                .assetId("8UUXN2Q00A01FZ")
                .snNumber("8UUXN2Q00A01FZ")
                .currentProgress(0)
                .currentStep("CREATED")
                .breakReason(null)
                .build();

        // The exact call createTask() makes internally.
        TaskProtoDTO wireRequest = MissionAutonomyImpl.mapTaskDtoToProto(
                TaskProtoDTO.newBuilder(), taskDTO).build();

        System.out.println("taskConfigCase = " + wireRequest.getTaskConfigCase());
        System.out.println("wireRequest = " + wireRequest);

        assertEquals(TaskProtoDTO.TaskConfigCase.WAYPOINT_CONFIG, wireRequest.getTaskConfigCase(),
                "the real client-sdk mapper did not put the waypoint config on the wire");
        assertEquals(4, wireRequest.getWaypointConfig().getWaypointsCount(),
                "the real client-sdk mapper lost waypoints");
        assertEquals(40.0f, wireRequest.getWaypointConfig().getGlobalHeight(), 0.001f);
        assertEquals("ererse", wireRequest.getName());
        assertEquals("75d334fa-3690-436a-99c7-fdebb4c36547", wireRequest.getMissionId());
    }

    private static WaypointDTO waypoint(double lat, double lon, float alt, int order) {
        return WaypointDTO.builder()
                .latitude(lat).longitude(lon).altitude(alt).speed(10.0f)
                .flyThrough(true).vehicleAction(VehicleAction.VEHICLE_ACTION_NONE)
                .wpOrder(order).gimbalPitch(-45)
                .build();
    }
}
