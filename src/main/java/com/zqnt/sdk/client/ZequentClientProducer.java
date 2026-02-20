package com.zqnt.sdk.client;

import com.zqnt.sdk.client.config.GrpcClientConfig;
import com.zqnt.sdk.client.config.ZequentClientConfigFactory;
import com.zqnt.sdk.client.grpc.ChannelFactory;
import com.zqnt.sdk.client.livedata.application.LiveData;
import com.zqnt.sdk.client.livedata.application.impl.LiveDataImpl;
import com.zqnt.sdk.client.missionautonomy.application.MissionAutonomy;
import com.zqnt.sdk.client.missionautonomy.application.impl.MissionAutonomyImpl;
import com.zqnt.sdk.client.remotecontrol.application.RemoteControl;
import com.zqnt.sdk.client.remotecontrol.application.impl.RemoteControlImpl;

import io.grpc.ManagedChannel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * CDI Producer for ZequentClient - the single entry point for customers.
 * This creates the ZequentClient bean with all services configured via properties.
 */
@Slf4j
@ApplicationScoped
public class ZequentClientProducer {


    private final ZequentClientConfigFactory configFactory;

	public ZequentClientProducer(ZequentClientConfigFactory configFactory) {
		this.configFactory = configFactory;
	}

	/**
     * Produces the ZequentClient bean - the ONLY public API for customers.
     * All service implementations are internal and not exposed as separate beans.
     */
    @Produces
    @ApplicationScoped
    public ZequentClient produceZequentClient() {
        log.info("Creating ZequentClient from properties");

        // Create config from properties
        GrpcClientConfig config = configFactory.createConfig();

        // Create channels for each service
        List<ManagedChannel> channels = new ArrayList<>();
        ManagedChannel remoteControlChannel = ChannelFactory.createChannel(config.getRemoteControlConfig());
        ManagedChannel missionAutonomyChannel = ChannelFactory.createChannel(config.getMissionAutonomyConfig());
        ManagedChannel liveDataChannel = ChannelFactory.createChannel(config.getLiveDataConfig());
        channels.add(remoteControlChannel);
        channels.add(missionAutonomyChannel);
        channels.add(liveDataChannel);

        // Create service implementations (internal, not exposed as beans)
        RemoteControl remoteControl =RemoteControlImpl.create(config, remoteControlChannel);
        MissionAutonomy missionAutonomy = MissionAutonomyImpl.create(config, missionAutonomyChannel);
        LiveData liveData = LiveDataImpl.create(config, liveDataChannel);

        // Create and return ZequentClient
        return new ZequentClient(config, remoteControl, missionAutonomy, liveData, channels);
    }
}
