package org.tn5250j.session.client;

import org.tn5250j.session.api.transport.SessionEventSink;
import org.tn5250j.session.api.transport.SessionTransport;
import org.tn5250j.session.wire.WsEnvelope;

import java.util.concurrent.CompletableFuture;

/**
 * No-op transport for in-process sessions. Wire messages are not used.
 */
public final class InProcessTransport implements SessionTransport {

    public static final InProcessTransport INSTANCE = new InProcessTransport();

    private InProcessTransport() {
    }

    @Override
    public void connect(SessionEventSink sink) {
    }

    @Override
    public CompletableFuture<WsEnvelope> send(WsEnvelope envelope) {
        return CompletableFuture.completedFuture(
                WsEnvelope.error(envelope.getId(), "In-process transport does not support wire commands"));
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void close() {
    }
}
