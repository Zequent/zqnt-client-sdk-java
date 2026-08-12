package com.zqnt.sdk.client.missionautonomy.application.impl;

import com.zqnt.sdk.client.ZequentClient;
import com.zqnt.sdk.client.missionautonomy.domains.TaskResponse;
import com.zqnt.utils.mission.proto.*;
import com.zqnt.utils.missionautonomy.domains.TaskDTO;
import com.zqnt.utils.missionautonomy.domains.WaypointDTO;
import com.zqnt.utils.missionautonomy.domains.config.WaypointTaskConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real service/DB round-trip for the customer waypoint-task payload.
 *
 * <p>It verifies the SDK wire mapping, the create response and a fresh getTask response (which
 * forces a second DB read). The target mission can be selected with
 * {@code integration.test.mission-id}.</p>
 *
 * <pre>
 * mvn -Dtest=WaypointTaskPersistenceIntegrationTest \
 *     -Dtest.excludedGroups= \
 *     -Dintegration.tests.enabled=true test
 * </pre>
 */
@Tag("integration")
@EnabledIfSystemProperty(named = "integration.tests.enabled", matches = "true")
class WaypointTaskPersistenceIntegrationTest {

    private static final UUID TEST_MISSION_ID = UUID.fromString(System.getProperty(
            "integration.test.mission-id", "75d334fa-3690-436a-99c7-fdebb4c36547"));

    private static ZequentClient client;

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    @BeforeAll
    static void createClient() {
        client = ZequentClient.builder()
                .missionAutonomy()
                    .host(System.getProperty("mission.autonomy.test.host", "localhost"))
                    .port(Integer.getInteger("mission.autonomy.test.port", 8004))
                    .usePlaintext(true)
                    .useStork(false)
                    .done()
                .maxRetryAttempts(3)
                .retryDelayMillis(500)
                .connectionTimeoutSeconds(5)
                .requestTimeoutSeconds(15)
                .build();
    }

    @AfterAll
    static void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void persistsEveryWaypointTaskFieldAndChecksKnownWireDifferences() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String taskId = null;

        try {
            TaskDTO request = customerWaypointTask(TEST_MISSION_ID, suffix);

            // This is the exact production mapping used immediately before the gRPC request.
            TaskProtoDTO wireTask = MissionAutonomyImpl
                    .mapTaskDtoToProto(TaskProtoDTO.newBuilder(), request)
                    .build();
            assertWireMapping(request, wireTask);

            TaskResponse createResponse = client.missionAutonomy()
                    .createTask(request)
                    .get(20, TimeUnit.SECONDS);
            assertSuccessfulTaskResponse(createResponse);
            taskId = createResponse.getTaskId();
            assertNotNull(taskId);
            assertFalse(taskId.isBlank());
            assertTaskRoundTrip(request, createResponse.getTaskData(), taskId, "createTask response");

            // A separate service call proves the value survived persistence and was rebuilt from DB data.
            TaskResponse getResponse = client.missionAutonomy()
                    .getTask(taskId)
                    .get(20, TimeUnit.SECONDS);
            assertSuccessfulTaskResponse(getResponse);
            assertEquals(taskId, getResponse.getTaskId());
            assertTaskRoundTrip(request, getResponse.getTaskData(), taskId, "getTask DB response");
        } finally {
            if (taskId != null) {
                TaskResponse deleteTask = client.missionAutonomy()
                        .deleteTask(taskId)
                        .get(20, TimeUnit.SECONDS);
                assertSuccessfulTaskResponse(deleteTask);
            }
        }
    }

    private static TaskDTO customerWaypointTask(UUID missionId, String suffix) {
        WaypointTaskConfig config = WaypointTaskConfig.builder()
                .externalTaskId("ad9fcc54-e13c-4dce-b729-01dec12820d4-" + suffix)
                .waypoints(List.of(
                        waypoint(47.776500625, 9.267688194, 40.0f, 0),
                        waypoint(47.776583782, 9.268981112, 40.0f, 1),
                        waypoint(47.775779670, 9.267862663, 40.0f, 2),
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
                .payloadImagingType("wide-angle")
                .fileUrl("https://example.invalid/wayline.kmz")
                .fileMd5("0123456789abcdef0123456789abcdef")
                .flightAreaFileUrl("https://example.invalid/flight-area.json")
                .flightAreaChecksum("abcdef0123456789")
                .build();

        return TaskDTO.builder()
                .missionId(missionId)
                .name("ererse-" + suffix)
                .description("desc")
                .taskType(TaskTypeProto.TASK_TYPE_WAYPOINT)
                .config(config)
                .externalTaskId("sdk-task-" + suffix)
                .executionOrder(3)
                .decisionEngineEnabled(true)
                .status(TaskStatus.TASK_DRAFT)
                .assetId(System.getProperty("integration.test.sn", "8UUXN2Q00A01FZ"))
                .snNumber(System.getProperty("integration.test.sn", "8UUXN2Q00A01FZ"))
                .currentProgress(7)
                .currentStep("CREATED")
                .breakReason(FlighttaskBreakReasonEnumProto.BREAK_REASON_USER_EXIT)
                .modifiedFrom("client-sdk-integration-test")
                .build();
    }

    private static WaypointDTO waypoint(double latitude, double longitude, float altitude, int order) {
        return WaypointDTO.builder()
                .latitude(latitude)
                .longitude(longitude)
                .altitude(altitude)
                .speed(10.0f)
                .flyThrough(true)
                .vehicleAction(VehicleAction.VEHICLE_ACTION_NONE)
                .wpOrder(order)
                .gimbalPitch(-45)
                .build();
    }

    private static void assertWireMapping(TaskDTO expected, TaskProtoDTO actual) {
        assertAll("TaskDTO -> TaskProtoDTO mapping",
                () -> assertEquals(expected.getMissionId().toString(), actual.getMissionId()),
                () -> assertEquals(expected.getName(), actual.getName()),
                () -> assertEquals(expected.getDescription(), actual.getDescription()),
                () -> assertEquals(expected.getTaskType(), actual.getTaskType()),
                () -> assertEquals(expected.getExternalTaskId(), actual.getExternalTaskId()),
                () -> assertEquals(expected.getExecutionOrder(), actual.getExecutionOrder()),
                () -> assertEquals(expected.getDecisionEngineEnabled(), actual.getDecisionEngineEnabled()),
                () -> assertEquals(expected.getStatus(), actual.getStatus()),
                () -> assertEquals(expected.getAssetId(), actual.getAssetId()),
                () -> assertEquals(expected.getSnNumber(), actual.getSnNumber()),
                () -> assertEquals(expected.getCurrentProgress(), actual.getCurrentProgress()),
                () -> assertEquals(expected.getCurrentStep(), actual.getCurrentStep()),
                () -> assertEquals(expected.getBreakReason(), actual.getBreakReason()),
                () -> assertEquals(expected.getModifiedFrom(), actual.getModifiedFrom()),
                () -> assertTrue(actual.hasWaypointConfig(), "waypoint_config oneof is missing"),
                () -> assertTrue(actual.getWaypointConfig().getWaypointsList().stream()
                                .noneMatch(WaypointProtoDTO::hasFlyTrough),
                        "flyThrough unexpectedly mapped despite the fly_trough schema spelling"),
                () -> assertWaypointConfig(
                        (WaypointTaskConfig) expected.getConfig(),
                        MissionAutonomyImpl.mapTaskProtoToDto(actual).getConfig(),
                        "wire mapping"));
    }

    private static void assertTaskRoundTrip(
            TaskDTO expected, TaskDTO actual, String expectedTaskId, String stage) {
        assertNotNull(actual, stage + " did not contain taskData");
        assertAll(stage,
                () -> assertEquals(UUID.fromString(expectedTaskId), actual.getId(), "server-generated id"),
                () -> assertNotNull(actual.getCreatedAt(), "server-generated createdAt"),
                () -> assertNotNull(actual.getModifiedAt(), "server-generated modifiedAt"),
                () -> assertEquals(expected.getMissionId(), actual.getMissionId(), "missionId"),
                () -> assertEquals(expected.getName(), actual.getName(), "name"),
                () -> assertEquals(expected.getDescription(), actual.getDescription(), "description"),
                () -> assertEquals(expected.getTaskType(), actual.getTaskType(), "taskType"),
                () -> assertEquals(expected.getExternalTaskId(), actual.getExternalTaskId(), "externalTaskId"),
                () -> assertEquals(expected.getExecutionOrder(), actual.getExecutionOrder(), "executionOrder"),
                () -> assertEquals(expected.getDecisionEngineEnabled(), actual.getDecisionEngineEnabled(),
                        "decisionEngineEnabled"),
                () -> assertEquals(expected.getStatus(), actual.getStatus(), "status"),
                () -> assertEquals(expected.getAssetId(), actual.getAssetId(), "assetId"),
                () -> assertEquals(expected.getSnNumber(), actual.getSnNumber(), "snNumber"),
                () -> assertEquals(expected.getCurrentProgress(), actual.getCurrentProgress(), "currentProgress"),
                () -> assertEquals(expected.getCurrentStep(), actual.getCurrentStep(), "currentStep"),
                () -> assertEquals(expected.getBreakReason(), actual.getBreakReason(), "breakReason"),
                () -> assertEquals(expected.getModifiedFrom(), actual.getModifiedFrom(), "modifiedFrom"),
                () -> assertEquals(expected.getTaskConfigTemplate(), actual.getTaskConfigTemplate(),
                        "taskConfigTemplate"),
                () -> assertEquals(expected.getAutonomyConfig(), actual.getAutonomyConfig(), "autonomyConfig"),
                () -> assertWaypointConfig(expected.getConfig(), actual.getConfig(), stage));
    }

    private static void assertWaypointConfig(Object expectedConfig, Object actualConfig, String stage) {
        WaypointTaskConfig expected = assertInstanceOf(WaypointTaskConfig.class, expectedConfig);
        WaypointTaskConfig actual = assertInstanceOf(WaypointTaskConfig.class, actualConfig,
                stage + " did not return WaypointTaskConfig");
        assertAll(stage + " waypoint config",
                () -> assertEquals(expected.getExternalTaskId(), actual.getExternalTaskId(), "externalTaskId"),
                () -> assertEquals(expected.getFlyToWaylineMode(), actual.getFlyToWaylineMode(),
                        "flyToWaylineMode"),
                () -> assertEquals(expected.getWaylineFinishAction(), actual.getWaylineFinishAction(),
                        "waylineFinishAction"),
                () -> assertEquals(expected.getWaylineType(), actual.getWaylineType(), "waylineType"),
                () -> assertEquals(expected.getWaylineTurnMode(), actual.getWaylineTurnMode(),
                        "waylineTurnMode"),
                () -> assertEquals(expected.getUseStraightLine(), actual.getUseStraightLine(), "useStraightLine"),
                () -> assertEquals(expected.getWaylinePrecisionType(), actual.getWaylinePrecisionType(),
                        "waylinePrecisionType"),
                () -> assertEquals(expected.getExitWaylineWhenRcLostEnum(), actual.getExitWaylineWhenRcLostEnum(),
                        "exitWaylineWhenRcLostEnum"),
                () -> assertEquals(expected.getRcLostActionEnum(), actual.getRcLostActionEnum(),
                        "rcLostActionEnum"),
                () -> assertEquals(expected.getOutOfControlAction(), actual.getOutOfControlAction(),
                        "outOfControlAction"),
                () -> assertEquals(expected.getTakeOffSecurityHeight(), actual.getTakeOffSecurityHeight(),
                        "takeOffSecurityHeight"),
                () -> assertEquals(expected.getRthAltitude(), actual.getRthAltitude(), "rthAltitude"),
                () -> assertEquals(expected.getRthMode(), actual.getRthMode(), "rthMode"),
                () -> assertEquals(expected.getRthSpeed(), actual.getRthSpeed(), "rthSpeed"),
                () -> assertEquals(expected.getGlobalSpeed(), actual.getGlobalSpeed(), "globalSpeed"),
                () -> assertEquals(expected.getGlobalTransitionSpeed(), actual.getGlobalTransitionSpeed(),
                        "globalTransitionSpeed"),
                () -> assertEquals(expected.getGlobalHeight(), actual.getGlobalHeight(), "globalHeight"),
                () -> assertEquals(expected.getGimbalPitchMode(), actual.getGimbalPitchMode(), "gimbalPitchMode"),
                () -> assertEquals(expected.getGlobalGimbalPitch(), actual.getGlobalGimbalPitch(),
                        "globalGimbalPitch"),
                () -> assertEquals(expected.getPayloadImagingType(), actual.getPayloadImagingType(),
                        "payloadImagingType"),
                () -> assertEquals(expected.getFileUrl(), actual.getFileUrl(), "fileUrl"),
                () -> assertEquals(expected.getFileMd5(), actual.getFileMd5(), "fileMd5"),
                () -> assertEquals(expected.getFlightAreaFileUrl(), actual.getFlightAreaFileUrl(),
                        "flightAreaFileUrl"),
                () -> assertEquals(expected.getFlightAreaChecksum(), actual.getFlightAreaChecksum(),
                        "flightAreaChecksum"),
                () -> assertEquals(expected.getWaypoints().size(), actual.getWaypoints().size(),
                        "waypoint count"));
        for (int index = 0; index < expected.getWaypoints().size(); index++) {
            WaypointDTO expectedWaypoint = expected.getWaypoints().get(index);
            WaypointDTO actualWaypoint = actual.getWaypoints().get(index);
            int waypointIndex = index;
            assertAll(stage + " waypoint[" + waypointIndex + "]",
                    () -> assertEquals(expectedWaypoint.getLatitude(), actualWaypoint.getLatitude(), "latitude"),
                    () -> assertEquals(expectedWaypoint.getLongitude(), actualWaypoint.getLongitude(), "longitude"),
                    () -> assertEquals(expectedWaypoint.getAltitude(), actualWaypoint.getAltitude(), "altitude"),
                    () -> assertEquals(expectedWaypoint.getSpeed(), actualWaypoint.getSpeed(), "speed"),
                    // Proto spells this field fly_trough, while the Java DTO uses flyThrough. The
                    // JSON-based production mapper therefore cannot carry it across the boundary.
                    () -> assertNull(actualWaypoint.getFlyThrough(),
                            "known difference: flyThrough is not mapped to proto fly_trough"),
                    () -> assertEquals(expectedWaypoint.getVehicleAction(), actualWaypoint.getVehicleAction(),
                            "vehicleAction"),
                    () -> assertEquals(expectedWaypoint.getWpOrder(), actualWaypoint.getWpOrder(), "wpOrder"),
                    () -> assertEquals(expectedWaypoint.getGimbalPitch(), actualWaypoint.getGimbalPitch(),
                            "gimbalPitch"));
        }
    }

    private static void assertSuccessfulTaskResponse(TaskResponse response) {
        assertNotNull(response);
        assertNotNull(response.getTid());
        assertNotNull(response.getTimestamp());
        assertNull(response.getError(), () -> "Task response contains an error despite success="
                + response.isSuccess() + ": " + response.getError().getErrorCode() + ": "
                + response.getError().getErrorMessage());
        assertTrue(response.isSuccess(), () -> response.getError() != null
                ? response.getError().getErrorCode() + ": " + response.getError().getErrorMessage()
                : "Task operation failed");
    }

}
