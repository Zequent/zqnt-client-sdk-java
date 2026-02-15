package com.zequent.framework.client.sdk.config.properties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration properties for ZequentClient.
 * Can be configured via application.properties or environment variables.
 *
 * Example application.properties:
 * <pre>
 * zequent.remote-control-service.host=localhost
 * zequent.remote-control-service.port=9091
 * zequent.remote-control-service.use-stork=false
 * </pre>
 *
 * Example environment variables:
 * <pre>
 * ZEQUENT_REMOTE_CONTROL_SERVICE_HOST=localhost
 * ZEQUENT_REMOTE_CONTROL_SERVICE_PORT=9091
 * </pre>
 */
@ConfigMapping(prefix = "zequent")
public interface ZequentClientProperties {

    /**
     * Remote Control Service configuration.
     */
    @WithName("remote-control-service")
    ServiceProperties remoteControlService();

    /**
     * Mission Autonomy Service configuration.
     */
    @WithName("mission-autonomy-service")
    ServiceProperties missionAutonomyService();

    /**
     * Live Data Service configuration.
     */
    @WithName("live-data-service")
    ServiceProperties liveDataService();

    /**
     * Global resilience configuration.
     */
    ResilienceProperties resilience();

    /**
     * Configuration for an individual service.
     */
    interface ServiceProperties {

        @WithDefault("localhost")
        String host();

        @WithDefault("9090")
        int port();

        @WithDefault("true")
        boolean usePlaintext();

        @WithDefault("false")
        boolean useStork();

        @WithDefault("")
        String storkServiceName();

        @WithDefault("ROUND_ROBIN")
        String loadBalancerType();
    }

    /**
     * Global resilience configuration.
     */
    interface ResilienceProperties {

        @WithDefault("3")
        int maxRetryAttempts();

        @WithDefault("1000")
        long retryDelayMillis();

        @WithDefault("5")
        int circuitBreakerFailureThreshold();

        @WithDefault("30000")
        long circuitBreakerWaitDurationMillis();

        @WithDefault("30")
        int connectionTimeoutSeconds();

        @WithDefault("60")
        int requestTimeoutSeconds();
    }
}
