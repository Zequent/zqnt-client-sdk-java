package com.zqnt.sdk.client.remotecontrol.application.impl;

import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.zqnt.sdk.client.remotecontrol.domains.CustomCommandRequest;
import com.zqnt.sdk.client.remotecontrol.domains.CustomCommandResponse;
import com.zqnt.sdk.client.remotecontrol.domains.RemoteControlResponse;
import com.zqnt.utils.common.proto.RequestBase;
import com.zqnt.utils.core.ProtobufHelpers;
import com.zqnt.utils.devicecontrol.proto.CapabilityTarget;
import com.zqnt.utils.devicecontrol.proto.CapabilityTargetType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class CustomCommandMapper {

    com.zqnt.utils.devicecontrol.proto.CustomCommandRequest toProto(CustomCommandRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireText(request.getSn(), "sn");
        requireText(request.getCommandType(), "commandType");

        RequestBase.Builder base = RequestBase.newBuilder()
                .setSn(request.getSn())
                .setTid(hasText(request.getTid()) ? request.getTid() : UUID.randomUUID().toString())
                .setTimestamp(ProtobufHelpers.now());
        if (hasText(request.getAssetId())) base.setAssetId(request.getAssetId());

        var builder = com.zqnt.utils.devicecontrol.proto.CustomCommandRequest.newBuilder()
                .setBase(base)
                .setCommandId(request.getCommandType())
                .setParams(mapToStruct(request.getParams()));
        if (hasText(request.getComponentId())) {
            builder.setTarget(CapabilityTarget.newBuilder()
                    .setType(toProto(request.getTargetType()))
                    .setTargetRef(request.getComponentId()));
        }
        return builder.build();
    }

    private CapabilityTargetType toProto(
            com.zqnt.sdk.client.remotecontrol.domains.CapabilityTargetType type) {
        if (type == null) return CapabilityTargetType.CAPABILITY_TARGET_TYPE_PAYLOAD;
        return switch (type) {
            case ASSET -> CapabilityTargetType.CAPABILITY_TARGET_TYPE_ASSET;
            case SUB_ASSET -> CapabilityTargetType.CAPABILITY_TARGET_TYPE_SUB_ASSET;
            case PAYLOAD -> CapabilityTargetType.CAPABILITY_TARGET_TYPE_PAYLOAD;
            case COMPONENT -> CapabilityTargetType.CAPABILITY_TARGET_TYPE_COMPONENT;
        };
    }

    CustomCommandResponse fromProto(com.zqnt.utils.devicecontrol.proto.CustomCommandResponse proto, String sn) {
        var meta = proto.getMeta();
        return CustomCommandResponse.builder()
                .success(!proto.getHasErrors())
                .sn(sn)
                .tid(meta.getTid())
                .assetId(meta.hasAssetId() ? meta.getAssetId() : null)
                .message(meta.hasResponseMessage() ? meta.getResponseMessage() : null)
                .commandType(proto.getCommandId())
                .result(proto.hasResult() ? structToMap(proto.getResult()) : Map.of())
                .error(proto.hasError() ? RemoteControlResponse.ErrorInfo.builder()
                        .errorCode(proto.getError().getErrorCode().name())
                        .errorMessage(proto.getError().getErrorMessage())
                        .timestamp(ProtobufHelpers.toLocalDateTime(proto.getError().getTimestamp()))
                        .build() : null)
                .progress(proto.hasProgress() ? RemoteControlResponse.ProgressInfo.builder()
                        .progress(proto.getProgress().getProgress())
                        .state(proto.getProgress().getState())
                        .leftTimeInSeconds(proto.getProgress().getLeftTimeInSeconds())
                        .build() : null)
                .build();
    }

    private Struct mapToStruct(Map<String, Object> values) {
        Struct.Builder builder = Struct.newBuilder();
        if (values == null) return builder.build();
        values.forEach((key, value) -> {
            requireText(key, "params key");
            builder.putFields(key, objectToValue(value));
        });
        return builder.build();
    }

    private Value objectToValue(Object value) {
        if (value == null) return Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
        if (value instanceof String text) return Value.newBuilder().setStringValue(text).build();
        if (value instanceof Boolean bool) return Value.newBuilder().setBoolValue(bool).build();
        if (value instanceof Number number) {
            double numericValue = number.doubleValue();
            if (!Double.isFinite(numericValue)) {
                throw new IllegalArgumentException("params numeric values must be finite");
            }
            return Value.newBuilder().setNumberValue(numericValue).build();
        }
        if (value instanceof Enum<?> enumValue) {
            return Value.newBuilder().setStringValue(enumValue.name()).build();
        }
        if (value instanceof Map<?, ?> map) {
            Struct.Builder nested = Struct.newBuilder();
            map.forEach((key, item) -> {
                if (!(key instanceof String text)) {
                    throw new IllegalArgumentException("nested params keys must be strings");
                }
                requireText(text, "nested params key");
                nested.putFields(text, objectToValue(item));
            });
            return Value.newBuilder().setStructValue(nested).build();
        }
        if (value instanceof Iterable<?> iterable) {
            ListValue.Builder list = ListValue.newBuilder();
            iterable.forEach(item -> list.addValues(objectToValue(item)));
            return Value.newBuilder().setListValue(list).build();
        }
        throw new IllegalArgumentException("Unsupported custom command param type: "
                + value.getClass().getName());
    }

    private Map<String, Object> structToMap(Struct struct) {
        Map<String, Object> result = new LinkedHashMap<>();
        struct.getFieldsMap().forEach((key, value) -> result.put(key, valueToObject(value)));
        return result;
    }

    private Object valueToObject(Value value) {
        return switch (value.getKindCase()) {
            case NULL_VALUE, KIND_NOT_SET -> null;
            case NUMBER_VALUE -> value.getNumberValue();
            case STRING_VALUE -> value.getStringValue();
            case BOOL_VALUE -> value.getBoolValue();
            case STRUCT_VALUE -> structToMap(value.getStructValue());
            case LIST_VALUE -> value.getListValue().getValuesList().stream()
                    .map(this::valueToObject).toList();
        };
    }

    private void requireText(String value, String fieldName) {
        if (!hasText(value)) throw new IllegalArgumentException(fieldName + " must not be null or blank");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
