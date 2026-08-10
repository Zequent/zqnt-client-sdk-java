package com.zqnt.sdk.client.missionautonomy.capabilities;

import com.google.protobuf.Struct;
import com.zqnt.utils.devicecontrol.proto.CapabilityTarget;
import com.zqnt.utils.execution.proto.CapabilityExecutionOptionsProto;
import com.zqnt.utils.execution.proto.CapabilityExecutionSpecProto;
import com.zqnt.utils.execution.proto.SimpleExecutionSpecProto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CapabilityApiTest {

    @Test
    void createsAutoStartingSimpleExecution() {
        var parameters = Struct.newBuilder().putFields("altitude",
                com.google.protobuf.Value.newBuilder().setNumberValue(30).build()).build();

        var command = CapabilityExecutionCommand.simple("drone-1", "flight.takeoff",
                CapabilityTarget.getDefaultInstance(), parameters, "takeoff-42");

        assertEquals("drone-1", command.assetSn());
        assertEquals("takeoff-42", command.idempotencyKey());
        assertEquals("flight.takeoff", command.spec().getSimple().getCommandId());
        assertEquals(30, command.spec().getSimple().getParameters()
                .getFieldsOrThrow("altitude").getNumberValue());
        assertTrue(command.options().getAutoStart());
    }

    @Test
    void createsVersionedPackageExecution() {
        var command = CapabilityExecutionCommand.packaged("drone-1", "inspection", "perimeter",
                "2.1.0", Struct.getDefaultInstance(), "exec-1");

        assertTrue(command.spec().hasPackage());
        assertEquals("inspection", command.spec().getPackage().getPackageId());
        assertEquals("perimeter", command.spec().getPackage().getCapabilityId());
        assertEquals("2.1.0", command.spec().getPackage().getPackageVersion());
    }

    @Test
    void rejectsInvalidCommandsAndQueriesBeforeNetworkCall() {
        var validSpec = CapabilityExecutionSpecProto.newBuilder().setSimple(
                SimpleExecutionSpecProto.newBuilder().setCommandId("flight.takeoff")).build();

        assertThrows(IllegalArgumentException.class, () -> new CapabilityExecutionCommand(
                " ", validSpec, CapabilityExecutionOptionsProto.getDefaultInstance(), "key",
                null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new CapabilityExecutionCommand(
                "drone-1", CapabilityExecutionSpecProto.getDefaultInstance(),
                CapabilityExecutionOptionsProto.getDefaultInstance(), "key", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new CapabilityPackageQuery(
                null, null, 201, null));
        assertThrows(IllegalArgumentException.class, () -> new CapabilitySignalCommand(
                "exec-1", null, null, null, null, null));
    }

    @Test
    void pagesAreImmutableAndExposeContinuation() {
        var mutable = new ArrayList<>(List.of("first"));
        var page = new CapabilityPage<>(mutable, "next");
        mutable.add("second");

        assertEquals(List.of("first"), page.items());
        assertTrue(page.hasNextPage());
        assertThrows(UnsupportedOperationException.class, () -> page.items().add("third"));
    }
}
