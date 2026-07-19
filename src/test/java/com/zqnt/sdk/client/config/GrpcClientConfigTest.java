package com.zqnt.sdk.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrpcClientConfigTest {

    @Test
    void streamDefaultsAreSafeForLongLivedSubscriptions() {
        GrpcClientConfig config = GrpcClientConfig.builder().build();

        assertEquals(300, config.getStreamInactivityTimeoutSeconds());
        assertEquals(35, config.getTelemetryHeartbeatTimeoutSeconds());
        assertEquals(2, config.getLiveDataSchedulerThreads());
    }
}
