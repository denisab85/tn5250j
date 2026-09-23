package org.tn5250j.session.server;

import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.tn5250j.framework.tn5250.Screen5250;
import org.tn5250j.session.api.host.SessionOpenRequest;
import org.tn5250j.session.api.transport.SessionEventSink;
import org.tn5250j.session.rpc.RpcContext;
import org.tn5250j.session.rpc.RpcRegistry;
import org.tn5250j.session.wire.WsEnvelope;
import org.tn5250j.session.wire.WsMessageCodec;
import org.tn5250j.session.wire.WsMessageType;
import org.tn5250j.tools.logging.SessionDebugLog;
import org.tn5250j.tools.logging.TN5250jLogFactory;
import org.tn5250j.tools.logging.TN5250jLogger;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class WebSocketSessionServer extends WebSocketServer {

    private static final TN5250jLogger LOG = TN5250jLogFactory.getLogger(WebSocketSessionServer.class);

    private final DefaultSessionHostService hostService = new DefaultSessionHostService();
    private final RpcRegistry rpcRegistry = new RpcRegistry();
    private final String authToken;
    private final InetSocketAddress listenAddress;
    private final CountDownLatch startupLatch = new CountDownLatch(1);
    private final Map<String, CompletableFuture<WsEnvelope>> pending = new ConcurrentHashMap<>();
    private final Map<WebSocket, ConnectionState> connections = new ConcurrentHashMap<>();

    public WebSocketSessionServer(InetSocketAddress address, String authToken) {
        super(address);
        setReuseAddr(true);
        this.listenAddress = address;
        this.authToken = authToken == null ? "" : authToken;
        registerRpcHandlers();
    }

    /** Blocks until {@link #onStart()} runs or the timeout elapses. */
    public boolean awaitStartup(long timeout, TimeUnit unit) throws InterruptedException {
        return startupLatch.await(timeout, unit);
    }

    public DefaultSessionHostService getHostService() {
        return hostService;
    }

    private void registerRpcHandlers() {
        rpcRegistry.register("terminal.systemRequest", context -> {
            SessionBridge bridge = hostService.getBridge(context.getSessionId());
            JsonObject result = new JsonObject();
            if (bridge == null) {
                result.addProperty("error", "Unknown session");
                return result;
            }
            if (bridge.getSession().getVT() == null) {
                result.addProperty("error", "VT unavailable");
                return result;
            }
            String code = context.getArgs().get("code").getAsString();
            if (code.length() == 1) {
                bridge.getSession().getVT().systemRequest(code.charAt(0));
            } else {
                bridge.getSession().getVT().systemRequest(code);
            }
            result.addProperty("ok", true);
            return result;
        });
        rpcRegistry.register("terminal.toggleDebug", context -> {
            SessionBridge bridge = hostService.getBridge(context.getSessionId());
            JsonObject result = new JsonObject();
            if (bridge != null && bridge.getSession().getVT() != null) {
                bridge.getSession().getVT().toggleDebug();
                result.addProperty("ok", true);
            } else {
                result.addProperty("error", "VT unavailable");
            }
            return result;
        });
        rpcRegistry.register("screen.setResetRequired", context ->
                applyScreenOption(context, screen -> screen.setResetRequired(
                        context.getArgs().get("value").getAsBoolean())));
        rpcRegistry.register("screen.setBackspaceError", context ->
                applyScreenOption(context, screen -> screen.setBackspaceError(
                        context.getArgs().get("value").getAsBoolean())));
        rpcRegistry.register("screen.checkHotSpots", context -> {
            SessionBridge bridge = hostService.getBridge(context.getSessionId());
            JsonObject result = new JsonObject();
            if (bridge == null) {
                result.addProperty("error", "Unknown session");
                return result;
            }
            boolean found = bridge.getSession().getScreen().checkHotSpots();
            result.addProperty("found", found);
            result.addProperty("ok", true);
            return result;
        });
        rpcRegistry.register("screen.copyTextField", context -> {
            SessionBridge bridge = hostService.getBridge(context.getSessionId());
            JsonObject result = new JsonObject();
            if (bridge == null) {
                result.addProperty("error", "Unknown session");
                return result;
            }
            int position = context.getArgs().get("position").getAsInt();
            result.addProperty("text", bridge.getSession().getScreen().copyTextField(position));
            result.addProperty("ok", true);
            return result;
        });
    }

    private JsonObject applyScreenOption(RpcContext context, Consumer<Screen5250> action) {
        SessionBridge bridge = hostService.getBridge(context.getSessionId());
        JsonObject result = new JsonObject();
        if (bridge == null) {
            result.addProperty("error", "Unknown session");
            return result;
        }
        action.accept(bridge.getSession().getScreen());
        result.addProperty("ok", true);
        return result;
    }

    @Override
    public void onStart() {
        LOG.info("tn5250j session server listening on ws://" + listenAddress.getHostString()
                + ":" + listenAddress.getPort());
        startupLatch.countDown();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.put(conn, new ConnectionState());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        WsEnvelope envelope = WsMessageCodec.decode(message);
        if (WsMessageType.HOST_HELLO.equals(envelope.getType())) {
            handleHello(conn, envelope);
            return;
        }
        ConnectionState state = connections.get(conn);
        if (state == null || !state.authenticated) {
            send(conn, WsEnvelope.error(envelope.getId(), "Not authenticated"));
            return;
        }
        if (envelope.getCorrelationId() != null && isResponseType(envelope.getType())) {
            completePending(envelope);
            return;
        }
        handleCommand(conn, state, envelope);
    }

    private void handleHello(WebSocket conn, WsEnvelope envelope) {
        ConnectionState state = connections.get(conn);
        if (state == null) {
            return;
        }
        String token = envelope.getPayload() != null && envelope.getPayload().has("token")
                ? envelope.getPayload().get("token").getAsString() : "";
        if (!authToken.isEmpty() && !authToken.equals(token)) {
            send(conn, WsEnvelope.error(envelope.getId(), "Authentication failed"));
            conn.close();
            return;
        }
        state.authenticated = true;
        JsonObject payload = new JsonObject();
        payload.addProperty("protocolVersion", WsEnvelope.PROTOCOL_VERSION);
        send(conn, WsEnvelope.reply(envelope.getId(), payload));
    }

    private void handleCommand(WebSocket conn, ConnectionState state, WsEnvelope envelope) {
        String type = envelope.getType();
        try {
            switch (type) {
                case WsMessageType.SESSION_OPEN:
                    handleSessionOpen(conn, state, envelope);
                    break;
                case WsMessageType.SESSION_CLOSE:
                    hostService.closeSession(envelope.getSessionId());
                    send(conn, WsEnvelope.reply(envelope.getId(), new JsonObject()));
                    break;
                case WsMessageType.SESSION_ATTACH:
                    handleSessionAttach(conn, state, envelope);
                    break;
                case WsMessageType.CONNECT:
                    requireBridge(envelope).connect();
                    send(conn, WsEnvelope.reply(envelope.getId(), new JsonObject()));
                    break;
                case WsMessageType.DISCONNECT:
                    requireBridge(envelope).disconnect();
                    send(conn, WsEnvelope.reply(envelope.getId(), new JsonObject()));
                    break;
                case WsMessageType.SEND_KEYS: {
                    String keys = envelope.getPayload().get("keys").getAsString();
                    SessionDebugLog.keyStroke("server", "received", keys);
                    requireBridge(envelope).getSession().getScreen().sendKeys(keys);
                    send(conn, WsEnvelope.reply(envelope.getId(), new JsonObject()));
                    break;
                }
                case WsMessageType.PASTE_TEXT: {
                    JsonObject pastePayload = envelope.getPayload();
                    String content = pastePayload.get("content").getAsString();
                    boolean special = pastePayload.get("special").getAsBoolean();
                    SessionDebugLog.keyStroke("server", "paste", "special=" + special + " len=" + content.length());
                    requireBridge(envelope).getSession().getScreen().pasteText(content, special);
                    send(conn, requireBridge(envelope).snapshotReply(envelope.getId()));
                    break;
                }
                case WsMessageType.MOVE_CURSOR: {
                    int pos = envelope.getPayload().get("pos").getAsInt();
                    SessionDebugLog.mouse("server", "received", "pos=" + pos);
                    boolean moved = requireBridge(envelope).getSession().getScreen().moveCursor(pos);
                    JsonObject moveReply = new JsonObject();
                    moveReply.addProperty("moved", moved);
                    send(conn, WsEnvelope.reply(envelope.getId(), moveReply));
                    break;
                }
                case WsMessageType.SET_CURSOR: {
                    int row = envelope.getPayload().get("row").getAsInt();
                    int col = envelope.getPayload().get("col").getAsInt();
                    SessionDebugLog.mouse("server", "setCursor", "row=" + row + " col=" + col);
                    requireBridge(envelope).getSession().getScreen().setCursor(row, col);
                    send(conn, WsEnvelope.reply(envelope.getId(), new JsonObject()));
                    break;
                }
                case WsMessageType.SEND_AID: {
                    int aidKey = envelope.getPayload().get("aidKey").getAsInt();
                    SessionDebugLog.keyStroke("server", "aid", "aidKey=" + aidKey);
                    requireBridge(envelope).getSession().getScreen().sendAid(aidKey);
                    send(conn, WsEnvelope.reply(envelope.getId(), new JsonObject()));
                    break;
                }
                case WsMessageType.GET_SNAPSHOT:
                    send(conn, requireBridge(envelope).snapshotReply(envelope.getId()));
                    break;
                case WsMessageType.UI_PROMPT_RESPONSE:
                    completePending(envelope);
                    break;
                case WsMessageType.RPC:
                    handleRpc(conn, envelope);
                    break;
                default:
                    send(conn, WsEnvelope.error(envelope.getId(), "Unknown command: " + type));
            }
        } catch (Exception ex) {
            send(conn, WsEnvelope.error(envelope.getId(), ex.getMessage()));
        }
    }

    private void handleSessionOpen(WebSocket conn, ConnectionState state, WsEnvelope envelope) {
        JsonObject payload = envelope.getPayload();
        Map<String, String> properties = new java.util.HashMap<>();
        if (payload.has("properties")) {
            JsonObject propsJson = payload.getAsJsonObject("properties");
            for (String key : propsJson.keySet()) {
                properties.put(key, propsJson.get(key).getAsString());
            }
        }
        SessionOpenRequest request = new SessionOpenRequest(
                payload.get("sessionName").getAsString(),
                payload.has("configurationResource")
                        ? payload.get("configurationResource").getAsString() : "",
                properties);
        String sessionId = hostService.openSession(request);
        state.sessionId = sessionId;
        attachBridge(conn, state, sessionId);
        JsonObject reply = new JsonObject();
        reply.addProperty("sessionId", sessionId);
        WsEnvelope response = WsEnvelope.reply(envelope.getId(), reply);
        response.setSessionId(sessionId);
        send(conn, response);
    }

    private void handleSessionAttach(WebSocket conn, ConnectionState state, WsEnvelope envelope) {
        String sessionId = envelope.getSessionId();
        state.sessionId = sessionId;
        attachBridge(conn, state, sessionId);
        send(conn, WsEnvelope.reply(envelope.getId(), new JsonObject()));
    }

    private void attachBridge(WebSocket conn, ConnectionState state, String sessionId) {
        SessionBridge bridge = hostService.getBridge(sessionId);
        if (bridge == null) {
            throw new IllegalArgumentException("Unknown session: " + sessionId);
        }
        SessionEventSink sink = new SessionEventSink() {
            @Override
            public void onMessage(WsEnvelope envelope) {
                send(conn, envelope);
            }

            @Override
            public void onClosed(String reason) {
            }

            @Override
            public void onError(Throwable error) {
            }
        };
        bridge.bind(sink, prompt -> {
            CompletableFuture<WsEnvelope> future = new CompletableFuture<>();
            pending.put(prompt.getId(), future);
            send(conn, prompt);
            return future;
        });
    }

    private SessionBridge requireBridge(WsEnvelope envelope) {
        SessionBridge bridge = hostService.getBridge(envelope.getSessionId());
        if (bridge == null) {
            throw new IllegalArgumentException("Unknown session: " + envelope.getSessionId());
        }
        return bridge;
    }

    private void handleRpc(WebSocket conn, WsEnvelope envelope) throws Exception {
        JsonObject payload = envelope.getPayload();
        String method = payload.get("method").getAsString();
        JsonObject args = payload.has("args") ? payload.getAsJsonObject("args") : new JsonObject();
        JsonObject result = rpcRegistry.invoke(new RpcContext(envelope.getSessionId(), method, args));
        send(conn, WsEnvelope.reply(envelope.getId(), result));
    }

    private void completePending(WsEnvelope envelope) {
        CompletableFuture<WsEnvelope> future = pending.remove(envelope.getCorrelationId());
        if (future != null) {
            future.complete(envelope);
        }
    }

    private boolean isResponseType(String type) {
        return WsMessageType.REPLY.equals(type)
                || WsMessageType.ERROR.equals(type)
                || WsMessageType.UI_PROMPT_RESPONSE.equals(type);
    }

    private void send(WebSocket conn, WsEnvelope envelope) {
        conn.send(WsMessageCodec.encode(envelope));
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            conn.close();
        }
    }

    private static final class ConnectionState {
        private boolean authenticated;
        private String sessionId;
    }
}
