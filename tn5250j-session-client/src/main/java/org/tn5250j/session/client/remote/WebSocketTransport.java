package org.tn5250j.session.client.remote;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.tn5250j.session.api.transport.SessionEventSink;
import org.tn5250j.session.api.transport.SessionTransport;
import org.tn5250j.session.wire.WsEnvelope;
import org.tn5250j.session.wire.WsMessageCodec;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class WebSocketTransport implements SessionTransport {

    private final URI uri;
    private final String authToken;
    private SessionEventSink sink;
    private WebSocketClient client;
    private final Map<String, CompletableFuture<WsEnvelope>> pending = new ConcurrentHashMap<>();

    public WebSocketTransport(URI uri, String authToken) {
        this.uri = uri;
        this.authToken = authToken == null ? "" : authToken;
    }

    @Override
    public synchronized void connect(SessionEventSink sink) throws Exception {
        this.sink = sink;
        CompletableFuture<Void> opened = new CompletableFuture<>();
        client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                opened.complete(null);
            }

            @Override
            public void onMessage(String message) {
                WsEnvelope envelope = WsMessageCodec.decode(message);
                if (envelope.getCorrelationId() != null) {
                    CompletableFuture<WsEnvelope> future = pending.remove(envelope.getCorrelationId());
                    if (future != null) {
                        future.complete(envelope);
                        return;
                    }
                }
                if (sink != null) {
                    sink.onMessage(envelope);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (WebSocketTransport.this.sink != null) {
                    WebSocketTransport.this.sink.onClosed(reason);
                }
            }

            @Override
            public void onError(Exception ex) {
                if (WebSocketTransport.this.sink != null) {
                    WebSocketTransport.this.sink.onError(ex);
                }
            }
        };
        client.connectBlocking();
        opened.get();
    }

    @Override
    public CompletableFuture<WsEnvelope> send(WsEnvelope envelope) {
        CompletableFuture<WsEnvelope> future = new CompletableFuture<>();
        if (client == null || !client.isOpen()) {
            future.completeExceptionally(new IllegalStateException("WebSocket is not connected"));
            return future;
        }
        pending.put(envelope.getId(), future);
        client.send(WsMessageCodec.encode(envelope));
        return future;
    }

    @Override
    public boolean isConnected() {
        return client != null && client.isOpen();
    }

    @Override
    public synchronized void close() {
        if (client != null) {
            client.close();
        }
    }
}
