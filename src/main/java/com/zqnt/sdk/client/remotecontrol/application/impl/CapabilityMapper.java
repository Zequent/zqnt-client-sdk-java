package com.zqnt.sdk.client.remotecontrol.application.impl;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.zqnt.sdk.client.remotecontrol.domains.CapabilityDescriptor;
import com.zqnt.sdk.client.remotecontrol.domains.CapabilitySnapshot;
import com.zqnt.sdk.client.remotecontrol.domains.CapabilityTargetType;
import com.zqnt.utils.core.ProtobufHelpers;
import com.zqnt.utils.devicecontrol.proto.AssetCapabilitiesResponse;

import java.util.LinkedHashMap;
import java.util.Map;

final class CapabilityMapper {
    CapabilitySnapshot fromProto(AssetCapabilitiesResponse response) {
        if (response.hasError()) {
            throw new IllegalStateException(response.getError().getErrorMessage());
        }
        var source = response.getCapabilities();
        return CapabilitySnapshot.builder()
                .assetSn(source.getAssetSn())
                .assetType(source.getAssetType())
                .revision(source.hasRevision() ? source.getRevision() : null)
                .snapshotState(source.getSnapshotState().name())
                .observedAt(ProtobufHelpers.toLocalDateTime(source.getTimestamp()))
                .validUntil(source.hasValidUntil()
                        ? ProtobufHelpers.toLocalDateTime(source.getValidUntil()) : null)
                .capabilities(source.getCapabilitiesList().stream().map(this::fromProto).toList())
                .build();
    }

    private CapabilityDescriptor fromProto(com.zqnt.utils.devicecontrol.proto.Capability source) {
        return CapabilityDescriptor.builder()
                .commandId(source.getCommandId()).displayName(source.getDisplayName())
                .description(source.hasDescription() ? source.getDescription() : null)
                .state(source.getState().name())
                .unavailableReason(source.hasUnavailableReason() ? source.getUnavailableReason() : null)
                .targetType(targetType(source.getTarget().getType()))
                .targetRef(source.getTarget().hasTargetRef() ? source.getTarget().getTargetRef() : null)
                .schemaVersion(source.hasSchemaVersion() ? source.getSchemaVersion() : null)
                .metadata(source.getMetadataMap()).constraints(toMap(source.getConstraints()))
                .inputSchema(toMap(source.getInputSchema())).outputSchema(toMap(source.getOutputSchema()))
                .build();
    }

    private CapabilityTargetType targetType(
            com.zqnt.utils.devicecontrol.proto.CapabilityTargetType type) {
        return switch (type) {
            case CAPABILITY_TARGET_TYPE_SUB_ASSET -> CapabilityTargetType.SUB_ASSET;
            case CAPABILITY_TARGET_TYPE_PAYLOAD -> CapabilityTargetType.PAYLOAD;
            case CAPABILITY_TARGET_TYPE_COMPONENT -> CapabilityTargetType.COMPONENT;
            default -> CapabilityTargetType.ASSET;
        };
    }

    private Map<String, Object> toMap(Struct struct) {
        Map<String, Object> result = new LinkedHashMap<>();
        struct.getFieldsMap().forEach((key, value) -> result.put(key, toObject(value)));
        return result;
    }

    private Object toObject(Value value) {
        return switch (value.getKindCase()) {
            case NULL_VALUE, KIND_NOT_SET -> null;
            case BOOL_VALUE -> value.getBoolValue();
            case NUMBER_VALUE -> value.getNumberValue();
            case STRING_VALUE -> value.getStringValue();
            case STRUCT_VALUE -> toMap(value.getStructValue());
            case LIST_VALUE -> value.getListValue().getValuesList().stream().map(this::toObject).toList();
        };
    }
}
