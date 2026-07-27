package com.zqnt.sdk.client.connector.application.impl;

import com.zqnt.sdk.client.connector.domains.ConnectorBatchSession;
import com.zqnt.sdk.client.connector.domains.ConnectorResponse;
import io.grpc.stub.StreamObserver;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

final class ConnectorBatchSessionImpl<T, P> implements ConnectorBatchSession<T> {

    private final StreamObserver<P> requestObserver;
    private final Function<T, P> mapper;
    private final CompletableFuture<ConnectorResponse> response;
    private final int timeoutSeconds;
    private final AtomicBoolean completed = new AtomicBoolean(false);

    ConnectorBatchSessionImpl(StreamObserver<P> requestObserver,
                              Function<T, P> mapper,
                              CompletableFuture<ConnectorResponse> response,
                              int timeoutSeconds) {
        this.requestObserver = requestObserver;
        this.mapper = mapper;
        this.response = response;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public void send(T request) {
        Objects.requireNonNull(request, "request must not be null");
        if (completed.get()) {
            throw new IllegalStateException("Connector batch session is already completed");
        }
        requestObserver.onNext(mapper.apply(request));
    }

    @Override
    public CompletableFuture<ConnectorResponse> complete() {
        if (completed.compareAndSet(false, true)) {
            requestObserver.onCompleted();
        }
        return response.orTimeout(timeoutSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void cancel(Throwable error) {
        if (completed.compareAndSet(false, true)) {
            Throwable cause = error != null ? error : new IllegalStateException("Connector batch cancelled");
            requestObserver.onError(cause);
            response.completeExceptionally(cause);
        }
    }

    @Override
    public boolean isCompleted() {
        return completed.get();
    }

    @Override
    public void close() {
        complete();
    }
}
