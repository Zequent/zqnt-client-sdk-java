package com.zqnt.sdk.client.missionautonomy.capabilities;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.zqnt.sdk.client.ZequentClient;
import com.zqnt.utils.execution.proto.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end test for a real, multi-node capability graph: registers a two-command capability
 * package (flight.takeoff -&gt; flight.return_to_home -&gt; end) with mission-autonomy, executes it
 * against a real asset, and polls the execution until it reaches a terminal status.
 *
 * <p>Unlike {@link com.zqnt.sdk.client.SimpleFlightIntegrationTest} (a single-node graph built
 * implicitly by RemoteControl), this exercises the full authoring path: capability package
 * registration ({@link com.zqnt.sdk.client.missionautonomy.application.MissionAutonomy#upsertCapabilityPackage})
 * -&gt; graph expansion/validation -&gt; packaged execution
 * ({@link com.zqnt.sdk.client.missionautonomy.application.MissionAutonomy#executeCapability}) -&gt;
 * status polling ({@link com.zqnt.sdk.client.missionautonomy.application.MissionAutonomy#getCapabilityExecution}).
 * The package is registered directly via mission-autonomy's gRPC API (no admin-console REST hop
 * needed), with {@code null} as the expected revision so repeated runs simply overwrite the
 * fixture instead of failing on an optimistic-concurrency conflict.</p>
 *
 * <p>Requires the full local stack running (connector, live-data, remote-control and
 * mission-autonomy services, plus an edge adapter registered for {@code integration.test.sn}) and
 * a valid license lease — {@code ExecuteCapability} is license-gated.</p>
 *
 * <pre>
 * mvn -Dtest=CapabilityGraphExecutionIntegrationTest \
 *     -Dtest.excludedGroups= \
 *     -Dintegration.tests.enabled=true \
 *     -Dintegration.test.sn=YOUR_DRONE_SN test
 * </pre>
 */
@Slf4j
@Tag("integration")
@EnabledIfSystemProperty(named = "integration.tests.enabled", matches = "true")
class CapabilityGraphExecutionIntegrationTest {

    private static final String TEST_ASSET_SN = System.getProperty(
            "integration.test.sn", "8UUXN2Q00A01FZ");
    private static final float TAKEOFF_LATITUDE = Float.parseFloat(
            System.getProperty("integration.test.takeoff.latitude", "47.775249"));
    private static final float TAKEOFF_LONGITUDE = Float.parseFloat(
            System.getProperty("integration.test.takeoff.longitude", "9.265800"));
    private static final float TAKEOFF_ALTITUDE = Float.parseFloat(
            System.getProperty("integration.test.takeoff.altitude", "30"));
    private static final long EXECUTION_TIMEOUT_SECONDS = Long.getLong(
            "integration.test.execution.timeout-seconds", 120L);
    private static final long POLL_INTERVAL_SECONDS = Long.getLong(
            "integration.test.execution.poll-interval-seconds", 3L);

    private static final String PACKAGE_ID = "zqnt.integration-test.simple-flight";
    private static final String PACKAGE_VERSION = "1.0.0";
    private static final String CAPABILITY_ID = "takeoff-then-return-home";

    private static ZequentClient client;

    static {
        // Avoid the JBoss LogManager warning when running the test directly from IntelliJ.
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
                .maxRetryAttempts(0)
                .requestTimeoutSeconds(15)
                .build();
    }

    @AfterAll
    static void closeClient() {
        if (client != null) client.close();
    }

    @Test
    void registersAndExecutesTwoNodeCapabilityGraph() throws Exception {
        CapabilityPackageProtoDTO upserted = client.missionAutonomy()
                .upsertCapabilityPackage(buildTakeoffThenReturnHomePackage(), null)
                .get(15, TimeUnit.SECONDS);
        assertEquals(PACKAGE_ID, upserted.getId());
        log.info("Registered capability package {}:{} (revision {})",
                PACKAGE_ID, PACKAGE_VERSION, upserted.getRevision());

        var command = CapabilityExecutionCommand.packaged(TEST_ASSET_SN, PACKAGE_ID, CAPABILITY_ID,
                PACKAGE_VERSION, Struct.getDefaultInstance(), "integration-test-" + UUID.randomUUID());
        CapabilityExecutionProtoDTO started = client.missionAutonomy().executeCapability(command)
                .get(15, TimeUnit.SECONDS);
        assertNotNull(started.getId());
        log.info("Started capability execution {} for {} (status {})",
                started.getId(), TEST_ASSET_SN, started.getStatus());

        CapabilityExecutionProtoDTO finished = awaitTerminalStatus(started.getId());
        finished.getNodeStatesList().forEach(node -> log.info("Node {} ({}) -> {}",
                node.getNodeId(), node.hasCommandId() ? node.getCommandId() : "n/a", node.getStatus()));

        assertEquals(CapabilityExecutionStatusProto.CAPABILITY_EXECUTION_STATUS_SUCCEEDED, finished.getStatus(),
                () -> finished.hasError()
                        ? finished.getError().getErrorCode() + ": " + finished.getError().getErrorMessage()
                        : "Execution " + finished.getId() + " ended with status " + finished.getStatus());
        log.info("Capability execution {} succeeded", finished.getId());
    }

    /** takeoff --success--> return-home --success--> end */
    private static CapabilityPackageProtoDTO buildTakeoffThenReturnHomePackage() {
        Struct takeoffParameters = Struct.newBuilder()
                .putFields("latitude", Value.newBuilder().setNumberValue(TAKEOFF_LATITUDE).build())
                .putFields("longitude", Value.newBuilder().setNumberValue(TAKEOFF_LONGITUDE).build())
                .putFields("altitude", Value.newBuilder().setNumberValue(TAKEOFF_ALTITUDE).build())
                .build();

        var takeoffNode = ExecutionNodeProtoDTO.newBuilder()
                .setId("takeoff").setName("Take off")
                .setType(ExecutionNodeTypeProto.EXECUTION_NODE_TYPE_COMMAND)
                .setCommand(CommandNodeConfigProto.newBuilder()
                        .setCommandId("flight.takeoff")
                        .setParameterDefaults(takeoffParameters));
        var returnHomeNode = ExecutionNodeProtoDTO.newBuilder()
                .setId("return-home").setName("Return to home")
                .setType(ExecutionNodeTypeProto.EXECUTION_NODE_TYPE_COMMAND)
                .setCommand(CommandNodeConfigProto.newBuilder().setCommandId("flight.return_to_home"));
        var endNode = ExecutionNodeProtoDTO.newBuilder()
                .setId("end").setName("End")
                .setType(ExecutionNodeTypeProto.EXECUTION_NODE_TYPE_END);

        var graph = ExecutionGraphProtoDTO.newBuilder()
                .setStartNodeId("takeoff")
                .addNodes(takeoffNode).addNodes(returnHomeNode).addNodes(endNode)
                .addEdges(ExecutionEdgeProtoDTO.newBuilder().setId("takeoff-to-return-home")
                        .setSourceNodeId("takeoff").setTargetNodeId("return-home")
                        .setType(ExecutionEdgeTypeProto.EXECUTION_EDGE_TYPE_SUCCESS))
                .addEdges(ExecutionEdgeProtoDTO.newBuilder().setId("return-home-to-end")
                        .setSourceNodeId("return-home").setTargetNodeId("end")
                        .setType(ExecutionEdgeTypeProto.EXECUTION_EDGE_TYPE_SUCCESS));

        var capability = CapabilityDefinitionProtoDTO.newBuilder()
                .setId(CAPABILITY_ID).setName("Takeoff then return home")
                .setDescription("Integration-test fixture: two-node capability graph chaining "
                        + "flight.takeoff -> flight.return_to_home.")
                .setGraph(graph);

        return CapabilityPackageProtoDTO.newBuilder()
                .setId(PACKAGE_ID).setVersion(PACKAGE_VERSION).setName("Zequent integration test package")
                .setEnabled(true)
                .addCapabilities(capability)
                .build();
    }

    private static CapabilityExecutionProtoDTO awaitTerminalStatus(String executionId) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(EXECUTION_TIMEOUT_SECONDS);
        CapabilityExecutionProtoDTO last = null;
        while (System.nanoTime() < deadline) {
            last = client.missionAutonomy().getCapabilityExecution(executionId).get(15, TimeUnit.SECONDS);
            if (isTerminal(last.getStatus())) return last;
            log.info("Execution {} still {} ({}%), polling again in {}s",
                    executionId, last.getStatus(), Math.round(last.getProgress() * 100), POLL_INTERVAL_SECONDS);
            TimeUnit.SECONDS.sleep(POLL_INTERVAL_SECONDS);
        }
        throw new AssertionError("Capability execution " + executionId + " did not reach a terminal status within "
                + EXECUTION_TIMEOUT_SECONDS + "s (last status: " + (last != null ? last.getStatus() : "unknown") + ")");
    }

    private static boolean isTerminal(CapabilityExecutionStatusProto status) {
        return switch (status) {
            case CAPABILITY_EXECUTION_STATUS_SUCCEEDED, CAPABILITY_EXECUTION_STATUS_FAILED,
                    CAPABILITY_EXECUTION_STATUS_CANCELLED -> true;
            default -> false;
        };
    }
}
