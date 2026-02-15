package com.zequent.framework.client.sdk.resilience;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Provides resilience patterns (retry, circuit breaker, timeout) for gRPC calls.
 */
@Slf4j
public class GrpcResilience {

    private final int maxRetryAttempts;
    private final long retryDelayMillis;
    private final int circuitBreakerFailureThreshold;
    private final long circuitBreakerWaitDurationMillis;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile long circuitOpenedAt = 0;
    private volatile boolean circuitOpen = false;

    public GrpcResilience(int maxRetryAttempts, long retryDelayMillis,
                          int circuitBreakerFailureThreshold, long circuitBreakerWaitDurationMillis) {
        this.maxRetryAttempts = maxRetryAttempts;
        this.retryDelayMillis = retryDelayMillis;
        this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
        this.circuitBreakerWaitDurationMillis = circuitBreakerWaitDurationMillis;
    }

    /**
     * Execute a gRPC call with retry and circuit breaker.
     */
    public <T> Uni<T> executeWithResilience(Uni<T> uni) {
        return checkCircuitBreaker()
                .chain(() -> uni
                        .onFailure().retry()
                        .withBackOff(Duration.ofMillis(retryDelayMillis))
                        .atMost(maxRetryAttempts)
                        .onFailure().invoke(this::recordFailure)
                        .onItem().invoke(item -> recordSuccess())
                );
    }

    /**
     * Execute a blocking call with retry and circuit breaker.
     */
    public <T> T executeBlocking(Supplier<T> supplier) {
        checkCircuitBreakerBlocking();

        int attempt = 0;
        Exception lastException = null;

        while (attempt <= maxRetryAttempts) {
            try {
                T result = supplier.get();
                recordSuccess();
                return result;
            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt <= maxRetryAttempts && isRetryable(e)) {
                    log.warn("Attempt {} failed, retrying after {} ms: {}",
                            attempt, retryDelayMillis, e.getMessage());
                    try {
                        Thread.sleep(retryDelayMillis * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                } else {
                    break;
                }
            }
        }

        recordFailure(lastException);
        throw new RuntimeException("All retry attempts failed", lastException);
    }

    private Uni<Void> checkCircuitBreaker() {
        if (circuitOpen) {
            long now = System.currentTimeMillis();
            if (now - circuitOpenedAt >= circuitBreakerWaitDurationMillis) {
                log.info("Circuit breaker attempting to close after wait duration");
                circuitOpen = false;
                failureCount.set(0);
            } else {
                return Uni.createFrom().failure(
                        new RuntimeException("Circuit breaker is OPEN - rejecting call"));
            }
        }
        return Uni.createFrom().voidItem();
    }

    private void checkCircuitBreakerBlocking() {
        if (circuitOpen) {
            long now = System.currentTimeMillis();
            if (now - circuitOpenedAt >= circuitBreakerWaitDurationMillis) {
                log.info("Circuit breaker attempting to close after wait duration");
                circuitOpen = false;
                failureCount.set(0);
            } else {
                throw new RuntimeException("Circuit breaker is OPEN - rejecting call");
            }
        }
    }

    private void recordSuccess() {
        if (failureCount.get() > 0) {
            log.info("Request succeeded - resetting failure count");
            failureCount.set(0);
        }
        if (circuitOpen) {
            log.info("Circuit breaker closing after successful request");
            circuitOpen = false;
        }
    }

    private void recordFailure(Throwable throwable) {
        int failures = failureCount.incrementAndGet();
        log.warn("Request failed - failure count: {}/{}", failures, circuitBreakerFailureThreshold);

        if (failures >= circuitBreakerFailureThreshold && !circuitOpen) {
            circuitOpen = true;
            circuitOpenedAt = System.currentTimeMillis();
            log.error("Circuit breaker OPENED after {} failures", failures);
        }
    }

    private boolean isRetryable(Exception e) {
        if (e instanceof StatusRuntimeException) {
            Status.Code code = ((StatusRuntimeException) e).getStatus().getCode();
            return code == Status.Code.UNAVAILABLE ||
                   code == Status.Code.DEADLINE_EXCEEDED ||
                   code == Status.Code.RESOURCE_EXHAUSTED ||
                   code == Status.Code.UNKNOWN;
        }
        return true; // Retry other exceptions
    }

    public boolean isCircuitOpen() {
        return circuitOpen;
    }

    public int getFailureCount() {
        return failureCount.get();
    }
}
