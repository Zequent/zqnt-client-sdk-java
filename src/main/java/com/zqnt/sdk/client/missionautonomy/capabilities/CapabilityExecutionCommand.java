package com.zqnt.sdk.client.missionautonomy.capabilities;

import com.google.protobuf.Struct;
import com.zqnt.utils.devicecontrol.proto.CapabilityTarget;
import com.zqnt.utils.execution.proto.CapabilityExecutionOptionsProto;
import com.zqnt.utils.execution.proto.CapabilityExecutionSpecProto;
import com.zqnt.utils.execution.proto.PackageExecutionSpecProto;
import com.zqnt.utils.execution.proto.SimpleExecutionSpecProto;

import java.util.Objects;
import java.util.UUID;

/** Type-safe input for creating or immediately executing a capability. */
public record CapabilityExecutionCommand(
        String assetSn,
        CapabilityExecutionSpecProto spec,
        CapabilityExecutionOptionsProto options,
        String idempotencyKey,
        String organizationId,
        String locationId,
        String theatreId) {

    public CapabilityExecutionCommand {
        assetSn = requireText(assetSn, "assetSn");
        spec = Objects.requireNonNull(spec, "spec must not be null");
        if (spec.getExecutionCase() == CapabilityExecutionSpecProto.ExecutionCase.EXECUTION_NOT_SET) {
            throw new IllegalArgumentException("spec must contain a simple or package execution");
        }
        options = options == null ? CapabilityExecutionOptionsProto.getDefaultInstance() : options;
        idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
    }

    public static CapabilityExecutionCommand simple(String assetSn, String commandId,
            CapabilityTarget target, Struct parameters, String idempotencyKey) {
        String command = requireText(commandId, "commandId");
        var simple = SimpleExecutionSpecProto.newBuilder()
                .setCommandId(command)
                .setTarget(target == null ? CapabilityTarget.getDefaultInstance() : target)
                .setParameters(parameters == null ? Struct.getDefaultInstance() : parameters)
                .build();
        return new CapabilityExecutionCommand(assetSn,
                CapabilityExecutionSpecProto.newBuilder().setSimple(simple).build(),
                CapabilityExecutionOptionsProto.newBuilder().setAutoStart(true).build(),
                defaultKey(idempotencyKey), null, null, null);
    }

    public static CapabilityExecutionCommand packaged(String assetSn, String packageId, String capabilityId,
            String packageVersion, Struct parameters, String idempotencyKey) {
        var packaged = PackageExecutionSpecProto.newBuilder()
                .setPackageId(requireText(packageId, "packageId"))
                .setCapabilityId(requireText(capabilityId, "capabilityId"))
                .setParameters(parameters == null ? Struct.getDefaultInstance() : parameters);
        if (packageVersion != null && !packageVersion.isBlank()) packaged.setPackageVersion(packageVersion);
        return new CapabilityExecutionCommand(assetSn,
                CapabilityExecutionSpecProto.newBuilder().setPackage(packaged).build(),
                CapabilityExecutionOptionsProto.newBuilder().setAutoStart(true).build(),
                defaultKey(idempotencyKey), null, null, null);
    }

    private static String defaultKey(String value) {
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }

    static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
