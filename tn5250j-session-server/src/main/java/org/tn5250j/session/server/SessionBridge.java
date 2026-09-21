package org.tn5250j.session.server;

import com.google.gson.JsonObject;
import org.tn5250j.Session5250;
import org.tn5250j.event.ScreenListener;
import org.tn5250j.event.ScreenOIAListener;
import org.tn5250j.event.SessionChangeEvent;
import org.tn5250j.event.SessionListener;
import org.tn5250j.framework.tn5250.InlineVtEventDispatcher;
import org.tn5250j.framework.tn5250.ScreenOIA;
import org.tn5250j.framework.tn5250.Screen5250;
import org.tn5250j.session.api.transport.SessionEventSink;
import org.tn5250j.session.server.wire.ScreenPlaneEncoder;
import org.tn5250j.session.server.wire.ScreenSnapshotBuilder;
import org.tn5250j.session.wire.OiaStateDto;
import org.tn5250j.session.wire.WsEnvelope;
import org.tn5250j.session.wire.WsMessageCodec;
import org.tn5250j.session.wire.WsMessageType;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class SessionBridge implements SessionListener, ScreenListener, ScreenOIAListener, AutoCloseable {

    private final String sessionId;
    private final Session5250 session;
    private SessionEventSink sink;
    private Function<WsEnvelope, CompletableFuture<WsEnvelope>> promptSender;

    public SessionBridge(String sessionId, Session5250 session) {
        this.sessionId = sessionId;
        this.session = session;
        session.addSessionListener(this);
        session.getScreen().addScreenListener(this);
        session.getScreen().getOIA().addOIAListener(this);
        session.setEventDispatcher(InlineVtEventDispatcher.INSTANCE);
    }

    public Session5250 getSession() {
        return session;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void bind(SessionEventSink sink, Function<WsEnvelope, CompletableFuture<WsEnvelope>> promptSender) {
        this.sink = sink;
        this.promptSender = promptSender;
        session.setUiHooks(new RemoteUiHooks(sessionId, promptSender));
    }

    public void connect() {
        session.connect();
    }

    public void disconnect() {
        session.disconnect();
    }

    public WsEnvelope snapshotReply(String correlationId) {
        JsonObject payload = WsMessageCodec.gson().toJsonTree(
                ScreenSnapshotBuilder.build(session.getScreen())).getAsJsonObject();
        WsEnvelope reply = WsEnvelope.reply(correlationId, payload);
        reply.setSessionId(sessionId);
        return reply;
    }

    @Override
    public void onSessionChanged(SessionChangeEvent changeEvent) {
        if (sink == null) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("state", changeEvent.getState());
        if (changeEvent.getMessage() != null) {
            payload.addProperty("message", changeEvent.getMessage());
        }
        sink.onMessage(WsEnvelope.event(WsMessageType.SESSION_STATE_CHANGED, sessionId, payload));
    }

    @Override
    public void onScreenChanged(int inUpdate, int startRow, int startCol, int endRow, int endCol) {
        if (sink == null) {
            return;
        }
        Screen5250 screen = session.getScreen();
        JsonObject payload = new JsonObject();
        payload.addProperty("inUpdate", inUpdate);
        payload.addProperty("startRow", startRow);
        payload.addProperty("startCol", startCol);
        payload.addProperty("endRow", endRow);
        payload.addProperty("endCol", endCol);
        payload.addProperty("currentRow", screen.getCurrentRow());
        payload.addProperty("currentCol", screen.getCurrentCol());
        payload.addProperty("cursorActive", screen.isCursorActive());
        payload.add("planes", WsMessageCodec.gson().toJsonTree(
                ScreenPlaneEncoder.encodeRegion(screen, startRow, startCol, endRow, endCol)));
        sink.onMessage(WsEnvelope.event(WsMessageType.SCREEN_REGION_UPDATED, sessionId, payload));
    }

    @Override
    public void onScreenSizeChanged(int rows, int cols) {
        if (sink == null) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("rows", rows);
        payload.addProperty("cols", cols);
        sink.onMessage(WsEnvelope.event(WsMessageType.SCREEN_SIZE_CHANGED, sessionId, payload));
    }

    @Override
    public void onOIAChanged(ScreenOIA oia, int change) {
        if (sink == null) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("change", change);
        OiaStateDto oiaState = ScreenSnapshotBuilder.buildOia(oia);
        payload.add("oia", WsMessageCodec.gson().toJsonTree(oiaState));
        sink.onMessage(WsEnvelope.event(WsMessageType.OIA_CHANGED, sessionId, payload));
    }

    @Override
    public void close() {
        session.getScreen().removeScreenListener(this);
        session.getScreen().getOIA().removeOIAListener(this);
        session.removeSessionListener(this);
        session.disconnect();
    }
}
