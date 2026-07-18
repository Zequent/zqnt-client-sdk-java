package com.zqnt.sdk.client.livedata.domains;

import io.grpc.stub.ClientCallStreamObserver;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Future;

/**
 * Handle for a running telemetry stream.
 * Call {@link #stop()} to cancel the stream and prevent any further reconnection attempts.
 */
public class StreamHandle implements AutoCloseable {

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Reference to the currently active gRPC call, rebound on every (re)connect.
     * Held so {@link #stop()} and internal reconnects can cancel the underlying wire-level
     * stream instead of leaving it open on the transport.
     */
    private final AtomicReference<ClientCallStreamObserver<?>> activeCall = new AtomicReference<>();
    private final AtomicReference<Future<?>> scheduledTask = new AtomicReference<>();

    /**
     * Stops the stream and cancels any pending reconnection attempts.
     */
    public void stop() {
        stopped.set(true);
        Future<?> task = scheduledTask.getAndSet(null);
        if (task != null) {
            task.cancel(false);
        }
        cancelActiveCall("stream stopped by client");
    }

    public boolean isStopped() {
        return stopped.get();
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * SDK-internal. Binds the gRPC call backing the current (re)connect so it can later be cancelled.
     * If the handle was already stopped before the call started (stop-before-start race),
     * the freshly bound call is cancelled immediately.
     */
    public void bindActiveCall(ClientCallStreamObserver<?> call) {
        activeCall.set(call);
        if (stopped.get()) {
            cancelActiveCall("stream stopped before start");
        }
    }

    /** SDK-internal. Tracks the watchdog or reconnect timer so stop() cancels it immediately. */
    public void bindScheduledTask(Future<?> task) {
        Future<?> previous = scheduledTask.getAndSet(task);
        if (previous != null && previous != task) {
            previous.cancel(false);
        }
        if (stopped.get()) {
            Future<?> current = scheduledTask.getAndSet(null);
            if (current != null) {
                current.cancel(false);
            }
        }
    }

    /**
     * SDK-internal. Cancels and clears the currently bound gRPC call, releasing its wire-level stream.
     * Cancelling an already-terminated call is a harmless no-op.
     */
    public void cancelActiveCall(String reason) {
        ClientCallStreamObserver<?> call = activeCall.getAndSet(null);
        if (call != null) {
            try {
                call.cancel(reason, null);
            } catch (Exception ignored) {
                // Cancelling a call that already completed/errored is a no-op; ignore.
            }
        }
    }
}
