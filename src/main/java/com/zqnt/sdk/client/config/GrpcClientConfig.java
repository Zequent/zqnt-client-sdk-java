package com.zequent.framework.client.sdk.config;

import lombok.Builder;
import lombok.Data;

/**
 * Global configuration for all gRPC client connections with resilience settings.
 */
@Data
@Builder
public class GrpcClientConfig {

    // Service-specific configurations
    private ServiceConfig remoteControlConfig;
    private ServiceConfig missionAutonomyConfig;
    private ServiceConfig liveDataConfig;

    // Global retry configuration
    @Builder.Default
    private int maxRetryAttempts = 3;

    @Builder.Default
    private long retryDelayMillis = 1000;

    // Global circuit breaker configuration
    @Builder.Default
    private int circuitBreakerFailureThreshold = 5;

    @Builder.Default
    private long circuitBreakerWaitDurationMillis = 30000; // 30 seconds

    // Global timeout configuration
    @Builder.Default
    private int connectionTimeoutSeconds = 30;

    @Builder.Default
    private int requestTimeoutSeconds = 60;

    // Default load balancer for all services
    @Builder.Default
    private ServiceConfig.LoadBalancerType defaultLoadBalancerType = ServiceConfig.LoadBalancerType.ROUND_ROBIN;
}
