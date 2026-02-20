package com.zequent.framework.client.sdk.config;

import com.zequent.framework.client.sdk.config.properties.ZequentClientProperties;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating ZequentClient configuration from properties.
 */
@Slf4j
@ApplicationScoped
public class ZequentClientConfigFactory {

    @Inject
    ZequentClientProperties properties;

    /**
     * Create a GrpcClientConfig from properties.
     */
    public GrpcClientConfig createConfig() {
        log.info("Creating ZequentClient configuration from properties");

        ServiceConfig remoteControlConfig = createServiceConfig(
                "remote-control",
                properties.remoteControlService(),
                9091
        );

        ServiceConfig missionAutonomyConfig = createServiceConfig(
                "mission-autonomy",
                properties.missionAutonomyService(),
                9092
        );

        ServiceConfig liveDataConfig = createServiceConfig(
                "live-data",
                properties.liveDataService(),
                9093
        );

        var resilience = properties.resilience();

        return GrpcClientConfig.builder()
                .remoteControlConfig(remoteControlConfig)
                .missionAutonomyConfig(missionAutonomyConfig)
                .liveDataConfig(liveDataConfig)
                .maxRetryAttempts(resilience.maxRetryAttempts())
                .retryDelayMillis(resilience.retryDelayMillis())
                .circuitBreakerFailureThreshold(resilience.circuitBreakerFailureThreshold())
                .circuitBreakerWaitDurationMillis(resilience.circuitBreakerWaitDurationMillis())
                .connectionTimeoutSeconds(resilience.connectionTimeoutSeconds())
                .requestTimeoutSeconds(resilience.requestTimeoutSeconds())
                .defaultLoadBalancerType(ServiceConfig.LoadBalancerType.ROUND_ROBIN)
                .build();
    }

    private ServiceConfig createServiceConfig(
            String serviceName,
            ZequentClientProperties.ServiceProperties props,
            int defaultPort) {

        int port = props.port() != 9090 ? props.port() : defaultPort;

        ServiceConfig.LoadBalancerType loadBalancerType;
        try {
            loadBalancerType = ServiceConfig.LoadBalancerType.valueOf(props.loadBalancerType());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid load balancer type '{}' for service '{}', using ROUND_ROBIN",
                    props.loadBalancerType(), serviceName);
            loadBalancerType = ServiceConfig.LoadBalancerType.ROUND_ROBIN;
        }

        String storkServiceName = props.storkServiceName();
        if (storkServiceName == null || storkServiceName.isEmpty()) {
            storkServiceName = serviceName + "-service";
        }

        ServiceConfig config = ServiceConfig.builder()
                .serviceName(serviceName)
                .host(props.host())
                .port(port)
                .usePlaintext(props.usePlaintext())
                .useStork(props.useStork())
                .storkServiceName(storkServiceName)
                .loadBalancerType(loadBalancerType)
                .build();

        log.info("Service '{}' configured: host={}, port={}, useStork={}, loadBalancer={}",
                serviceName, config.getHost(), config.getPort(),
                config.isUseStork(), config.getLoadBalancerType());

        return config;
    }
}
