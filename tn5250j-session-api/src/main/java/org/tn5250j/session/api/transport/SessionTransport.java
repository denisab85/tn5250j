package org.tn5250j.session.api.transport;

import org.tn5250j.session.wire.WsEnvelope;

import java.util.concurrent.CompletableFuture;

public interface SessionTransport extends AutoCloseable {

    void connect(SessionEventSink sink) throws Exception;

    CompletableFuture<WsEnvelope> send(WsEnvelope envelope);

    boolean isConnected();

    @Override
    void close();
}
