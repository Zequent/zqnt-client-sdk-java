package com.zequent.framework.client.sdk;

import com.zequent.framework.client.sdk.channel.ChannelFactory;
import com.zequent.framework.client.sdk.config.GrpcClientConfig;
import com.zequent.framework.client.sdk.config.ServiceConfig;
import com.zequent.framework.client.sdk.livedata.LiveData;
import com.zequent.framework.client.sdk.livedata.impl.LiveDataImpl;
import com.zequent.framework.client.sdk.missionautonomy.MissionAutonomy;
import com.zequent.framework.client.sdk.missionautonomy.impl.MissionAutonomyImpl;
import com.zequent.framework.client.sdk.remotecontrol.RemoteControl;
import com.zequent.framework.client.sdk.remotecontrol.impl.RemoteControlImpl;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Central Zequent Framework Client for interacting with all Zequent services.
 * Each service has its own connection, port, and load balancing configuration.
 * Supports Stork service discovery for Kubernetes/Docker deployments.
 *
 * This class is NOT a CDI bean itself - it's created by ZequentClientProducer.
 * Customers inject it using @Inject ZequentClient.
 *
 * Usage example:
 * <pre>
 * // CDI (recommended):
 * @Inject ZequentClient client;
 *
 * // Manual (for tests):
 * ZequentClient client = ZequentClient.builder()
 *     .remoteControl()
 *         .host("localhost")
 *         .port(9091)
 *         .useStork(false)
 *         .done()
 *     .liveData()
 *         .host("localhost")
 *         .port(9092)
 *         .done()
 *     .build();
 *
 * client.remoteControl().takeoff(...);
 * client.liveData().streamTelemetry(...);
 * </pre>
 */
@Slf4j
public class ZequentClient implements AutoCloseable {

    private final GrpcClientConfig config;
    private final RemoteControl remoteControl;
    private final MissionAutonomy missionAutonomy;
    private final LiveData liveData;
    private final List<ManagedChannel> channels;


    /**
     * Package-private constructor used by ZequentClientProducer.
     * Customers should NOT create instances directly - use @Inject instead!
     *
     * Note: This constructor is called by ZequentClientProducer, NOT by CDI directly.
     */
    ZequentClient(GrpcClientConfig config, RemoteControl remoteControl,
                  MissionAutonomy missionAutonomy, LiveData liveData,
                  List<ManagedChannel> channels) {
        this.config = config;
        this.remoteControl = remoteControl;
        this.missionAutonomy = missionAutonomy;
        this.liveData = liveData;
        this.channels = channels;
        log.info("ZequentClient initialized with {} channels", channels.size());
    }

    /**
     * Create a builder for configuring the ZequentClient (for standalone usage without CDI).
     * Most customers should use @Inject instead!
     *
     * @deprecated Use CDI injection with @Inject ZequentClient instead
     */
    @Deprecated
    public static ZequentClientBuilder builder() {
        return new ZequentClientBuilder();
    }

    /**
     * Access remote control operations for drones and docks.
     *
     * @return RemoteControl interface for flight operations, manual control, and dock operations
     */
    public RemoteControl remoteControl() {
        return remoteControl;
    }

    /**
     * Access mission autonomy operations.
     *
     * @return MissionAutonomy interface for mission planning and execution
     */
    public MissionAutonomy missionAutonomy() {
        return missionAutonomy;
    }

    /**
     * Access live telemetry data streaming.
     *
     * @return LiveData interface for streaming telemetry data
     */
    public LiveData liveData() {
        return liveData;
    }

    /**
     * Get the current configuration.
     */
    public GrpcClientConfig getConfig() {
        return config;
    }

    /**
     * Check if all channels are connected.
     */
    public boolean isConnected() {
        return channels.stream()
                .allMatch(ch -> !ch.isShutdown() && !ch.isTerminated());
    }

    @Override
    public void close() {
        log.info("Shutting down ZequentClient with {} channels", channels.size());
        for (ManagedChannel channel : channels) {
            try {
                channel.shutdown();
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Channel did not terminate gracefully, forcing shutdown");
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while shutting down channel", e);
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("All channels closed");
    }

    /**
     * Builder for creating configured ZequentClient instances with multi-service support.
     */
    public static class ZequentClientBuilder {
        // Global resilience settings
        private int maxRetryAttempts = 3;
        private long retryDelayMillis = 1000;
        private int circuitBreakerFailureThreshold = 5;
        private long circuitBreakerWaitDurationMillis = 30000;
        private int connectionTimeoutSeconds = 30;
        private int requestTimeoutSeconds = 60;
        private ServiceConfig.LoadBalancerType defaultLoadBalancerType = ServiceConfig.LoadBalancerType.ROUND_ROBIN;

        // Service-specific builders
        private ServiceConfigBuilder remoteControlBuilder;
        private ServiceConfigBuilder missionAutonomyBuilder;
        private ServiceConfigBuilder liveDataBuilder;

        public ZequentClientBuilder maxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
            return this;
        }

        public ZequentClientBuilder retryDelayMillis(long retryDelayMillis) {
            this.retryDelayMillis = retryDelayMillis;
            return this;
        }

        public ZequentClientBuilder circuitBreakerFailureThreshold(int threshold) {
            this.circuitBreakerFailureThreshold = threshold;
            return this;
        }

        public ZequentClientBuilder circuitBreakerWaitDurationMillis(long waitDuration) {
            this.circuitBreakerWaitDurationMillis = waitDuration;
            return this;
        }

        public ZequentClientBuilder connectionTimeoutSeconds(int timeout) {
            this.connectionTimeoutSeconds = timeout;
            return this;
        }

        public ZequentClientBuilder requestTimeoutSeconds(int timeout) {
            this.requestTimeoutSeconds = timeout;
            return this;
        }

        public ZequentClientBuilder defaultLoadBalancerType(ServiceConfig.LoadBalancerType type) {
            this.defaultLoadBalancerType = type;
            return this;
        }

        public ServiceConfigBuilder remoteControl() {
            this.remoteControlBuilder = new ServiceConfigBuilder(this, "remote-control");
            return this.remoteControlBuilder;
        }

        public ServiceConfigBuilder missionAutonomy() {
            this.missionAutonomyBuilder = new ServiceConfigBuilder(this, "mission-autonomy");
            return this.missionAutonomyBuilder;
        }

        public ServiceConfigBuilder liveData() {
            this.liveDataBuilder = new ServiceConfigBuilder(this, "live-data");
            return this.liveDataBuilder;
        }

        public ZequentClient build() {
            // Build service configs with defaults
            ServiceConfig remoteControlConfig = buildServiceConfig(remoteControlBuilder, "remote-control", 9091);
            ServiceConfig missionAutonomyConfig = buildServiceConfig(missionAutonomyBuilder, "mission-autonomy", 9092);
            ServiceConfig liveDataConfig = buildServiceConfig(liveDataBuilder, "live-data", 9093);

            // Build global config
            GrpcClientConfig globalConfig = GrpcClientConfig.builder()
                    .remoteControlConfig(remoteControlConfig)
                    .missionAutonomyConfig(missionAutonomyConfig)
                    .liveDataConfig(liveDataConfig)
                    .maxRetryAttempts(maxRetryAttempts)
                    .retryDelayMillis(retryDelayMillis)
                    .circuitBreakerFailureThreshold(circuitBreakerFailureThreshold)
                    .circuitBreakerWaitDurationMillis(circuitBreakerWaitDurationMillis)
                    .connectionTimeoutSeconds(connectionTimeoutSeconds)
                    .requestTimeoutSeconds(requestTimeoutSeconds)
                    .defaultLoadBalancerType(defaultLoadBalancerType)
                    .build();

            // Create channels for each service
            List<ManagedChannel> channels = new ArrayList<>();
            ManagedChannel remoteControlChannel = ChannelFactory.createChannel(remoteControlConfig);
            ManagedChannel missionAutonomyChannel = ChannelFactory.createChannel(missionAutonomyConfig);
            ManagedChannel liveDataChannel = ChannelFactory.createChannel(liveDataConfig);
            channels.add(remoteControlChannel);
            channels.add(missionAutonomyChannel);
            channels.add(liveDataChannel);

            // Create service implementations with their own channels
            RemoteControl remoteControl = RemoteControlImpl.create(globalConfig, remoteControlChannel);
            MissionAutonomy missionAutonomy = MissionAutonomyImpl.create(globalConfig, missionAutonomyChannel);
            LiveData liveData = LiveDataImpl.create(globalConfig, liveDataChannel);

            return new ZequentClient(globalConfig, remoteControl, missionAutonomy, liveData, channels);
        }

        private ServiceConfig buildServiceConfig(ServiceConfigBuilder builder, String serviceName, int defaultPort) {
            if (builder != null) {
                return builder.buildInternal();
            }
            // Default config
            return ServiceConfig.builder()
                    .serviceName(serviceName)
                    .host("localhost")
                    .port(defaultPort)
                    .usePlaintext(true)
                    .useStork(false)
                    .loadBalancerType(defaultLoadBalancerType)
                    .build();
        }
    }

    /**
     * Builder for configuring individual services.
     */
    public static class ServiceConfigBuilder {
        private final ZequentClientBuilder parent;
        private final String serviceName;
        private String host = "localhost";
        private Integer port;
        private boolean usePlaintext = true;
        private boolean useStork = false;
        private String storkServiceName;
        private ServiceConfig.LoadBalancerType loadBalancerType;

        ServiceConfigBuilder(ZequentClientBuilder parent, String serviceName) {
            this.parent = parent;
            this.serviceName = serviceName;
        }

        public ServiceConfigBuilder host(String host) {
            this.host = host;
            return this;
        }

        public ServiceConfigBuilder port(int port) {
            this.port = port;
            return this;
        }

        public ServiceConfigBuilder usePlaintext(boolean usePlaintext) {
            this.usePlaintext = usePlaintext;
            return this;
        }

        public ServiceConfigBuilder useStork(boolean useStork) {
            this.useStork = useStork;
            return this;
        }

        public ServiceConfigBuilder storkServiceName(String storkServiceName) {
            this.storkServiceName = storkServiceName;
            this.useStork = true;
            return this;
        }

        public ServiceConfigBuilder loadBalancerType(ServiceConfig.LoadBalancerType loadBalancerType) {
            this.loadBalancerType = loadBalancerType;
            return this;
        }

        public ZequentClientBuilder done() {
            return parent;
        }

        ServiceConfig buildInternal() {
            return ServiceConfig.builder()
                    .serviceName(serviceName)
                    .host(host)
                    .port(port != null ? port : getDefaultPort())
                    .usePlaintext(usePlaintext)
                    .useStork(useStork)
                    .storkServiceName(storkServiceName != null ? storkServiceName : serviceName + "-service")
                    .loadBalancerType(loadBalancerType != null ? loadBalancerType : parent.defaultLoadBalancerType)
                    .build();
        }

        private int getDefaultPort() {
            return switch (serviceName) {
                case "remote-control" -> 8002;
                case "mission-autonomy" -> 8004;
                case "live-data" -> 8003;
                default -> 9001;
            };
        }
    }
}
