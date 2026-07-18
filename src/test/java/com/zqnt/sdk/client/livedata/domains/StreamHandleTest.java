package com.zqnt.sdk.client.livedata.domains;

import org.junit.jupiter.api.Test;

import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamHandleTest {

    @Test
    void stopCancelsPendingTimer() {
        StreamHandle handle = new StreamHandle();
        FutureTask<Void> timer = new FutureTask<>(() -> null);
        handle.bindScheduledTask(timer);

        handle.stop();

        assertTrue(handle.isStopped());
        assertTrue(timer.isCancelled());
    }

    @Test
    void replacingTimerRemovesOldWatchdog() {
        StreamHandle handle = new StreamHandle();
        FutureTask<Void> watchdog = new FutureTask<>(() -> null);
        FutureTask<Void> reconnect = new FutureTask<>(() -> null);

        handle.bindScheduledTask(watchdog);
        handle.bindScheduledTask(reconnect);

        assertTrue(watchdog.isCancelled());
    }
}
