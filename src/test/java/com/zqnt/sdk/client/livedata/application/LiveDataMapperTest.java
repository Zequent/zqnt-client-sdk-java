package com.zqnt.sdk.client.livedata.application;

import com.google.protobuf.Timestamp;
import com.zqnt.sdk.client.livedata.domains.StreamNotificationResponse;
import com.zqnt.sdk.client.livedata.domains.StreamTelemetryResponse;
import com.zqnt.utils.livedata.proto.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LiveDataMapperTest {

    private static final Timestamp NOW = Timestamp.newBuilder().setSeconds(123).setNanos(456).build();

    @Test
    void mapsStreamHeartbeatSeparatelyFromTelemetry() {
        LiveDataTelemetryResponse proto = LiveDataTelemetryResponse.newBuilder()
                .setSn("*")
                .setTimestamp(NOW)
                .setStreamHeartbeat(LiveDataStreamHeartbeat.newBuilder().setTimestamp(NOW))
                .build();

        StreamTelemetryResponse mapped = LiveDataMapper.INSTANCE.fromProtoResponse(proto);

        assertEquals(StreamTelemetryResponse.StreamEventType.HEARTBEAT, mapped.getEventType());
        assertEquals(123, mapped.getStreamHeartbeat().getTimestamp().getEpochSecond());
        assertNull(mapped.getAssetTelemetry());
        assertNull(mapped.getSubAssetTelemetry());
    }

    @Test
    void mapsNoDataSourceStatusWithoutInventingLastTelemetryTimestamp() {
        TelemetrySourceStatus status = TelemetrySourceStatus.newBuilder()
                .setSn("EDGE-1")
                .setState(TelemetrySourceState.TELEMETRY_SOURCE_STATE_NO_DATA)
                .setObservedAt(NOW)
                .build();
        LiveDataTelemetryResponse proto = LiveDataTelemetryResponse.newBuilder()
                .setSn("EDGE-1")
                .setTimestamp(NOW)
                .setSourceStatus(status)
                .build();

        StreamTelemetryResponse mapped = LiveDataMapper.INSTANCE.fromProtoResponse(proto);

        assertEquals(StreamTelemetryResponse.StreamEventType.SOURCE_STATUS, mapped.getEventType());
        assertEquals(TelemetrySourceState.TELEMETRY_SOURCE_STATE_NO_DATA, mapped.getSourceStatus().getState());
        assertNull(mapped.getSourceStatus().getLastTelemetryAt());
    }

    @Test
    void notificationHeartbeatDoesNotBecomeDomainNotification() {
        LiveDataNotificationResponse proto = LiveDataNotificationResponse.newBuilder()
                .setSn("*")
                .setTimestamp(NOW)
                .setStreamHeartbeat(LiveDataStreamHeartbeat.newBuilder().setTimestamp(NOW))
                .build();

        StreamNotificationResponse mapped = LiveDataMapper.INSTANCE.fromProtoNotificationResponse(proto);

        assertNull(mapped.getEventType());
        assertNull(mapped.getAssetStatus());
        assertNull(mapped.getTaskEvent());
        assertNull(mapped.getOperationEvent());
        assertNull(mapped.getError());
    }
}
