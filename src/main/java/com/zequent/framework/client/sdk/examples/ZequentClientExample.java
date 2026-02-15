package com.zequent.framework.client.sdk.examples;

import com.zequent.framework.client.sdk.ZequentClient;
import com.zequent.framework.client.sdk.config.ServiceConfig;
import com.zequent.framework.client.sdk.models.TakeoffRequest;
import com.zequent.framework.client.sdk.models.TakeoffResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Example usage of the ZequentClient SDK with multi-service configuration.
 */
@Slf4j
public class ZequentClientExample {

    public static void main(String[] args) {
        multiServiceExample();
    }

    /**
     * Example: Multi-service configuration with different ports per service.
     */
    public static void multiServiceExample() {
        try (ZequentClient client = ZequentClient.builder()
                // Global resilience settings
                .maxRetryAttempts(3)
                .retryDelayMillis(1000)
                .circuitBreakerFailureThreshold(5)
                .defaultLoadBalancerType(ServiceConfig.LoadBalancerType.ROUND_ROBIN)

                // Remote Control Service on port 9091
                .remoteControl()
                    .host("localhost")
                    .port(9091)
                    .usePlaintext(true)
                    .done()

                // Live Data Service on port 9093
                .liveData()
                    .host("localhost")
                    .port(9093)
                    .done()

                // Mission Autonomy uses default (9092)
                .build()) {

            log.info("ZequentClient initialized with multi-service configuration");

            // Use Remote Control service (connects to port 9091)
            TakeoffRequest takeoffRequest = TakeoffRequest.builder()
                    .sn("1581F5FKD2389A00BS8E")
                    .assetId("asset-123")
                    .latitude(47.3769F)
                    .longitude(8.5417F)
                    .altitude(100.0F)
                    .build();

            TakeoffResponse response = client.remoteControl().takeoff(takeoffRequest).join();
            if (response.isSuccess()) {
                log.info("Takeoff successful: {}", response.getMessage());
            } else {
                log.error("Takeoff failed: {}", response.getError());
            }

            log.info("All operations completed");

        } catch (Exception e) {
            log.error("Error using ZequentClient", e);
        }
    }

    /**
     * Example: Simple client with defaults (all services on localhost with default ports).
     */
    public static void simpleExample() {
        try (ZequentClient client = ZequentClient.builder().build()) {

            // Uses defaults:
            // - remote-control: localhost:9091
            // - mission-autonomy: localhost:9092
            // - live-data: localhost:9093
            // - Retry: 3 attempts
            // - Circuit breaker: 5 failures threshold
            // - Load balancer: Round-robin

            var request = TakeoffRequest.builder()
                    .sn("1581F5FKD2389A00BS8E")
                    .latitude(47.3769F)
                    .longitude(8.5417F)
                    .altitude(100.0F)
                    .build();

            var response = client.remoteControl().takeoff(request).join();
            System.out.println("Success: " + response.isSuccess());
        }
    }

    /**
     * Example: Kubernetes/Docker deployment with Stork service discovery.
     */
    public static void kubernetesExample() {
        try (ZequentClient client = ZequentClient.builder()
                // Global settings
                .maxRetryAttempts(5)
                .defaultLoadBalancerType(ServiceConfig.LoadBalancerType.ROUND_ROBIN)

                // Remote Control with Stork
                .remoteControl()
                    .storkServiceName("remote-control-service")
                    .useStork(true)
                    .loadBalancerType(ServiceConfig.LoadBalancerType.ROUND_ROBIN)
                    .done()

                // Live Data with Stork
                .liveData()
                    .storkServiceName("live-data-service")
                    .useStork(true)
                    .done()

                // Mission Autonomy with Stork
                .missionAutonomy()
                    .storkServiceName("mission-autonomy-service")
                    .useStork(true)
                    .done()

                .build()) {

            log.info("Kubernetes client with Stork service discovery ready");

            // Stork will automatically:
            // - Discover service instances
            // - Load balance across instances
            // - Handle service updates/scaling
        }
    }

    /**
     * Example: Production with TLS and custom load balancing per service.
     */
    public static void productionExample() {
        try (ZequentClient client = ZequentClient.builder()
                // Global resilience
                .maxRetryAttempts(5)
                .retryDelayMillis(2000)
                .circuitBreakerFailureThreshold(10)
                .circuitBreakerWaitDurationMillis(60000)
                .connectionTimeoutSeconds(60)
                .requestTimeoutSeconds(120)

                // Remote Control with TLS
                .remoteControl()
                    .host("remote-control.zequent.com")
                    .port(443)
                    .usePlaintext(false)
                    .loadBalancerType(ServiceConfig.LoadBalancerType.LEAST_REQUESTS)
                    .done()

                // Live Data with TLS
                .liveData()
                    .host("live-data.zequent.com")
                    .port(443)
                    .usePlaintext(false)
                    .loadBalancerType(ServiceConfig.LoadBalancerType.ROUND_ROBIN)
                    .done()

                // Mission Autonomy with TLS
                .missionAutonomy()
                    .host("mission-autonomy.zequent.com")
                    .port(443)
                    .usePlaintext(false)
                    .done()

                .build()) {

            log.info("Production client ready with TLS and advanced load balancing");
        }
    }
}
