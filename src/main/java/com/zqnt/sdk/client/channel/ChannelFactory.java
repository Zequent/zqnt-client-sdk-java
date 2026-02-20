package com.zequent.framework.client.sdk.channel;

import com.zequent.framework.client.sdk.config.ServiceConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Factory for creating gRPC channels with load balancing and service discovery support.
 */
@Slf4j
public class ChannelFactory {

    /**
     * Create a managed channel for a service with the given configuration.
     */
    public static ManagedChannel createChannel(ServiceConfig config) {
        ManagedChannelBuilder<?> channelBuilder;

        if (config.isUseStork() && config.getStorkServiceName() != null) {
            // Use Stork service discovery
            log.info("Creating channel with Stork service discovery: {}", config.getStorkServiceName());
            channelBuilder = ManagedChannelBuilder.forTarget("stork://" + config.getStorkServiceName());
        } else {
            // Direct connection
            log.info("Creating direct channel: {}:{}", config.getHost(), config.getPort());
            channelBuilder = ManagedChannelBuilder.forAddress(config.getHost(), config.getPort());
        }

        // Configure load balancing
        String loadBalancerPolicy = getLoadBalancerPolicy(config.getLoadBalancerType());
        channelBuilder.defaultLoadBalancingPolicy(loadBalancerPolicy);
        log.info("Load balancer policy: {}", loadBalancerPolicy);

        // Configure keep-alive
        channelBuilder
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .idleTimeout(5, TimeUnit.MINUTES);

        // Configure TLS
        if (config.isUsePlaintext()) {
            channelBuilder.usePlaintext();
        }

        ManagedChannel channel = channelBuilder.build();
        log.info("Channel created successfully for service: {}", config.getServiceName());

        return channel;
    }

    private static String getLoadBalancerPolicy(ServiceConfig.LoadBalancerType type) {
        return switch (type) {
            case ROUND_ROBIN -> "round_robin";
            case RANDOM -> "random";
            case LEAST_REQUESTS -> "least_request";
            case POWER_OF_TWO_CHOICES -> "pick_first"; // Fallback
        };
    }
}
