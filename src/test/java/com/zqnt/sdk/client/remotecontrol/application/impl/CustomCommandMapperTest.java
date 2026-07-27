package com.zqnt.sdk.client.remotecontrol.application.impl;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.zqnt.sdk.client.remotecontrol.domains.CustomCommandRequest;
import com.zqnt.utils.common.proto.ResponseMeta;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomCommandMapperTest {

    private final CustomCommandMapper mapper = new CustomCommandMapper();

    @Test
    void mapsPojoCommandAndNestedParametersToProto() {
        var request = CustomCommandRequest.builder()
                .sn("asset-sn")
                .tid("transaction-1")
                .assetId("asset-1")
                .componentId("searchlight-1")
                .commandType("searchlight.mode.set")
                .params(Map.of(
                        "mode", 2,
                        "options", Map.of("group", 0),
                        "labels", List.of("night", "strobe")))
                .build();

        var proto = mapper.toProto(request);

        assertEquals("asset-sn", proto.getBase().getSn());
        assertEquals("transaction-1", proto.getBase().getTid());
        assertEquals("asset-1", proto.getBase().getAssetId());
        assertEquals("searchlight-1", proto.getComponentId());
        assertEquals("searchlight.mode.set", proto.getCommandType());
        assertEquals(2d, proto.getParams().getFieldsOrThrow("mode").getNumberValue());
        assertEquals(0d, proto.getParams().getFieldsOrThrow("options")
                .getStructValue().getFieldsOrThrow("group").getNumberValue());
        assertEquals(2, proto.getParams().getFieldsOrThrow("labels").getListValue().getValuesCount());
    }

    @Test
    void mapsCustomCommandResultToPojo() {
        Struct result = Struct.newBuilder()
                .putFields("accepted", Value.newBuilder().setBoolValue(true).build())
                .build();
        var proto = com.zqnt.utils.common.proto.CustomCommandResponse.newBuilder()
                .setHasErrors(false)
                .setCommandType("searchlight.mode.set")
                .setMeta(ResponseMeta.newBuilder()
                        .setTid("transaction-2")
                        .setSn("asset-sn")
                        .setAssetId("asset-1")
                        .setResponseMessage("accepted"))
                .setResult(result)
                .build();

        var response = mapper.fromProto(proto, "asset-sn");

        assertTrue(response.isSuccess());
        assertEquals("transaction-2", response.getTid());
        assertEquals("asset-1", response.getAssetId());
        assertEquals("searchlight.mode.set", response.getCommandType());
        assertEquals(true, response.getResult().get("accepted"));
    }

    @Test
    void rejectsUnsupportedOrInvalidParameterValues() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toProto(CustomCommandRequest.builder()
                .sn("asset-sn")
                .commandType("widget.set")
                .params(Map.of("value", Double.NaN))
                .build()));

        assertThrows(IllegalArgumentException.class, () -> mapper.toProto(CustomCommandRequest.builder()
                .sn("asset-sn")
                .commandType("widget.set")
                .params(Map.of("value", new Object()))
                .build()));
    }
}
