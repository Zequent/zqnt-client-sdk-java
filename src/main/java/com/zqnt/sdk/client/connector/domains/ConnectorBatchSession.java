package com.zqnt.sdk.client.connector.domains;

import java.util.concurrent.CompletableFuture;

/** A client-streaming Connector batch upload. */
public interface ConnectorBatchSession<T> extends AutoCloseable {

    void send(T request);

    /** Completes the request stream and resolves with the Connector response. */
    CompletableFuture<ConnectorResponse> complete();

    void cancel(Throwable error);

    boolean isCompleted();

    @Override
    void close();
}
