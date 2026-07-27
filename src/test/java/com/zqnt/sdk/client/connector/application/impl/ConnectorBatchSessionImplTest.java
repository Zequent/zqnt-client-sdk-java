package com.zqnt.sdk.client.connector.application.impl;

import com.zqnt.sdk.client.connector.domains.ConnectorResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ConnectorBatchSessionImplTest {

    @Test
    void sendsItemsAndCompletesTheRequestStream() {
        List<String> sent = new ArrayList<>();
        CompletableFuture<ConnectorResponse> response = new CompletableFuture<>();
        var session = new ConnectorBatchSessionImpl<String, String>(new StreamObserver<String>() {
            @Override public void onNext(String value) { sent.add(value); }
            @Override public void onError(Throwable error) { response.completeExceptionally(error); }
            @Override public void onCompleted() { response.complete(new ConnectorResponse()); }
        }, value -> value, response, 1);

        session.send("first");
        ConnectorResponse result = session.complete().join();

        assertEquals(List.of("first"), sent);
        assertEquals(new ConnectorResponse(), result);
        assertTrue(session.isCompleted());
        assertThrows(IllegalStateException.class, () -> session.send("late"));
    }
}
