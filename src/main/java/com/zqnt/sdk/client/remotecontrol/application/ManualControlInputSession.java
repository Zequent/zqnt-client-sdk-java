package com.zqnt.sdk.client.remotecontrol.application;

import com.zqnt.sdk.client.remotecontrol.domains.ManualControlInput;
import com.zqnt.sdk.client.remotecontrol.domains.RemoteControlResponse;

/**
 * Session for sending manual control input commands via gRPC streaming.
 * This allows the client to send multiple control inputs over a single stream connection.
 */
public interface ManualControlInputSession extends AutoCloseable {

    /**
     * Sends a single manual control input command over the stream.
     *
     * @param input The manual control input (roll, pitch, yaw, throttle, gimbalPitch)
     */
    void sendInput(ManualControlInput input);

    /**
     * Completes the stream and retrieves the final response from the server.
     *
     * @return The response from the remote control service
     */
    RemoteControlResponse complete();

    /**
     * Closes the stream with an error.
     *
     * @param error The error that occurred
     */
    void completeWithError(Throwable error);

    /**
     * Closes the stream and releases resources.
     */
    @Override
    void close();
}
