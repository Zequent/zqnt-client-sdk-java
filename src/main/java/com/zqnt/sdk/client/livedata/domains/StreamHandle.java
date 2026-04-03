package com.zqnt.sdk.client.livedata.domains;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handle for a running telemetry stream.
 * Call {@link #stop()} to cancel the stream and prevent any further reconnection attempts.
 */
public class StreamHandle implements AutoCloseable {

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Stops the stream and cancels any pending reconnection attempts.
     */
    public void stop() {
        stopped.set(true);
    }

    public boolean isStopped() {
        return stopped.get();
    }

    @Override
    public void close() {
        stop();
    }
}
