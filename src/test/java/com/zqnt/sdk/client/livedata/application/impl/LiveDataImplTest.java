package com.zqnt.sdk.client.livedata.application.impl;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveDataImplTest {

    @Test
    void reconnectDelayUsesExponentialBackoffWithJitterAndCap() {
        assertDelayInRange(1, 800, 1_200);
        assertDelayInRange(2, 1_600, 2_400);
        assertDelayInRange(3, 3_200, 4_800);
        assertDelayInRange(4, 6_400, 9_600);
        assertDelayInRange(20, 6_400, 9_600);
    }

    @Test
    void reconnectCannotStartSecondCallbackWhileOldAttemptIsStillProcessing() {
        LiveDataImpl.StreamSubscriptionState state = new LiveDataImpl.StreamSubscriptionState();

        assertTrue(state.tryBeginCallback(), "old connection owns the callback slot");
        assertFalse(state.tryBeginCallback(), "reconnected stream must drop while old callback runs");

        state.endCallback();
        assertTrue(state.tryBeginCallback(), "new connection can deliver after old callback finishes");
        state.endCallback();
    }

    @Test
    void separateSubscribersDoNotBlockEachOther() {
        LiveDataImpl.StreamSubscriptionState first = new LiveDataImpl.StreamSubscriptionState();
        LiveDataImpl.StreamSubscriptionState second = new LiveDataImpl.StreamSubscriptionState();

        assertTrue(first.tryBeginCallback());
        assertTrue(second.tryBeginCallback());

        first.endCallback();
        second.endCallback();
    }

    @Test
    void concurrentDeliveryKeepsAtMostOneCallbackPerSubscription() throws Exception {
        int contenders = 32;
        LiveDataImpl.StreamSubscriptionState state = new LiveDataImpl.StreamSubscriptionState();
        ExecutorService executor = Executors.newFixedThreadPool(contenders);
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch attempted = new CountDownLatch(contenders);
        CountDownLatch releaseWinner = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximumActive = new AtomicInteger();
        AtomicInteger accepted = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < contenders; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    boolean acquired = state.tryBeginCallback();
                    attempted.countDown();
                    if (!acquired) {
                        return null;
                    }
                    accepted.incrementAndGet();
                    int nowActive = active.incrementAndGet();
                    maximumActive.accumulateAndGet(nowActive, Math::max);
                    releaseWinner.await();
                    active.decrementAndGet();
                    state.endCallback();
                    return null;
                }));
            }

            ready.await();
            start.countDown();
            attempted.await();
            releaseWinner.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            releaseWinner.countDown();
            executor.shutdownNow();
        }

        assertEquals(1, accepted.get());
        assertEquals(1, maximumActive.get());
    }

    private void assertDelayInRange(int attempt, long minimum, long maximum) {
        for (int i = 0; i < 100; i++) {
            long delay = LiveDataImpl.reconnectDelayMillis(attempt, 1_000, 3);
            assertTrue(delay >= minimum && delay <= maximum,
                    () -> "delay " + delay + " outside [" + minimum + ", " + maximum + "]");
        }
    }
}
