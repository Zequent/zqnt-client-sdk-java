package com.zequent.framework.client.sdk.examples;

import com.zequent.framework.client.sdk.ZequentClient;
import com.zequent.framework.client.sdk.models.TakeoffRequest;
import com.zequent.framework.client.sdk.models.TakeoffResponse;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Example showing ZequentClient usage with property-based configuration.
 * Configuration is loaded from:
 * 1. application.properties (defaults)
 * 2. .env file (environment-specific overrides)
 * 3. Environment variables (deployment overrides)
 */
@Slf4j
public class PropertyBasedExample {

    @Inject
    ZequentClient client;

    /**
     * Example using CDI-injected client configured via properties.
     *
     * Configuration in application.properties or .env:
     * <pre>
     * REMOTE_CONTROL_SERVICE_HOST=localhost
     * REMOTE_CONTROL_SERVICE_PORT=9091
     * LIVE_DATA_SERVICE_HOST=localhost
     * LIVE_DATA_SERVICE_PORT=9093
     * </pre>
     */
    public void useInjectedClient() {
        log.info("Using property-configured ZequentClient");

        // Client is automatically configured from properties
        TakeoffRequest request = TakeoffRequest.builder()
                .sn("1581F5FKD2389A00BS8E")
                .latitude(47.3769F)
                .longitude(8.5417F)
                .altitude(100.0F)
                .build();

        TakeoffResponse response = client.remoteControl().takeoff(request).join();

        if (response.isSuccess()) {
            log.info("Takeoff successful: {}", response.getMessage());
        } else {
            log.error("Takeoff failed: {}", response.getError());
        }
    }

    /**
     * Example showing how customers switch environments without code changes.
     *
     * Development:
     * - Copy .env.dev.example to .env
     * - Run application
     *
     * Staging:
     * - Copy .env.staging.example to .env
     * - Run application
     *
     * Production:
     * - Set environment variables in Kubernetes
     * - Deploy application
     *
     * NO CODE CHANGES NEEDED!
     */
    public static void environmentSwitching() {
        log.info("""

                ============================================================
                ENVIRONMENT SWITCHING GUIDE
                ============================================================

                1. DEVELOPMENT:
                   cp .env.dev.example .env
                   mvn quarkus:dev

                   Services: localhost:9091, :9092, :9093

                2. STAGING (Docker Compose):
                   cp .env.staging.example .env
                   docker-compose up

                   Services: Use Docker service names

                3. PRODUCTION (Kubernetes):
                   Set environment variables in deployment.yaml:

                   env:
                     - name: REMOTE_CONTROL_SERVICE_USE_STORK
                       value: "true"
                     - name: REMOTE_CONTROL_SERVICE_STORK_NAME
                       value: "remote-control-service"

                   Services: Auto-discovered via Stork

                ============================================================
                NO CODE CHANGES NEEDED FOR ENVIRONMENT SWITCHING!
                ============================================================
                """);
    }
}
