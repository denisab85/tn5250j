package org.tn5250j.session.client.remote;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.tn5250j.session.api.ConnectionProfile;
import org.tn5250j.session.api.HeadlessSessionUiHooks;
import org.tn5250j.session.api.ScreenModel;
import org.tn5250j.session.api.SessionStateConstants;
import org.tn5250j.session.api.SessionClient;
import org.tn5250j.session.api.SessionListener;
import org.tn5250j.session.api.SessionUiHooks;
import org.tn5250j.session.api.TerminalOps;
import org.tn5250j.session.api.UnsupportedCapabilityException;
import org.tn5250j.session.api.transport.SessionEventSink;
import org.tn5250j.session.wire.OiaStateDto;
import org.tn5250j.session.wire.ScreenSnapshotDto;
import org.tn5250j.session.wire.WsEnvelope;
import org.tn5250j.session.wire.WsMessageCodec;
import org.tn5250j.session.wire.WsMessageType;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class RemoteSessionClient implements SessionClient, SessionEventSink {

    private final ConnectionProfile profile;
    private final WebSocketTransport transport;
    private final RemoteScreenModel screenModel;
    private final RemoteTerminalOps terminalOps;
    private final List<SessionListener> listeners = new ArrayList<>();

    private String sessionId;
    private SessionUiHooks uiHooks = HeadlessSessionUiHooks.INSTANCE;
    private boolean connected;

    public RemoteSessionClient(ConnectionProfile profile) {
        this.profile = profile;
        this.transport = new WebSocketTransport(URI.create(profile.getRemoteUrl()), profile.getAuthToken());
        this.screenModel = new RemoteScreenModel(
                this::sendKeysCommand,
                this::sendMoveCursor,
                this::sendAidCommand,
                () -> { });
        this.terminalOps = new RemoteTerminalOps(null, this::sendEnvelope);
    }

    public void setRepaintCallback(Runnable repaint) {
        screenModel.setRepaintFn(repaint);
    }

    public void open() throws Exception {
        transport.connect(this);
        JsonObject hello = new JsonObject();
        if (profile.getAuthToken() != null) {
            hello.addProperty("token", profile.getAuthToken());
        }
        WsEnvelope helloReply = transport.send(
                WsEnvelope.command(WsMessageType.HOST_HELLO, null, hello)).get();

        JsonObject openPayload = new JsonObject();
        openPayload.addProperty("sessionName", profile.getSessionName());
        openPayload.add("properties", WsMessageCodec.gson().toJsonTree(profile.getSessionPropertiesMap()));
        WsEnvelope openReply = transport.send(
                WsEnvelope.command(WsMessageType.SESSION_OPEN, null, openPayload)).get();
        sessionId = openReply.getPayload().get("sessionId").getAsString();
        terminalOps.setSessionId(sessionId);

        transport.send(WsEnvelope.command(WsMessageType.SESSION_ATTACH, sessionId, new JsonObject())).get();
        WsEnvelope snapshotReply = transport.send(
                WsEnvelope.command(WsMessageType.GET_SNAPSHOT, sessionId, new JsonObject())).get();
        applySnapshot(snapshotReply.getPayload());
    }

    private void applySnapshot(JsonObject payload) {
        ScreenSnapshotDto snapshot = WsMessageCodec.gson().fromJson(payload, ScreenSnapshotDto.class);
        screenModel.applySnapshot(snapshot);
    }

    private void refreshSnapshot() {
        if (sessionId == null) {
            return;
        }
        transport.send(WsEnvelope.command(WsMessageType.GET_SNAPSHOT, sessionId, new JsonObject()))
                .thenAccept(reply -> {
                    if (reply.getPayload() != null) {
                        applySnapshot(reply.getPayload());
                    }
                });
    }

    @Override
    public void connect() {
        try {
            if (sessionId == null) {
                open();
            }
            transport.send(WsEnvelope.command(WsMessageType.CONNECT, sessionId, new JsonObject())).get();
            connected = true;
            refreshSnapshot();
        } catch (Exception ex) {
            throw new IllegalStateException("Remote connect failed", ex);
        }
    }

    @Override
    public void disconnect() {
        try {
            transport.send(WsEnvelope.command(WsMessageType.DISCONNECT, sessionId, new JsonObject())).get();
        } catch (Exception ignored) {
        }
        connected = false;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String getSessionName() {
        return profile.getSessionName();
    }

    @Override
    public String getHostName() {
        return profile.getSessionProperties().getProperty("SESSION_HOST");
    }

    @Override
    public String getAllocatedDeviceName() {
        return null;
    }

    @Override
    public ScreenModel getScreen() {
        return screenModel;
    }

    @Override
    public void addSessionListener(SessionListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void sendKeys(String keys) {
        sendKeysCommand(keys);
    }

    private void sendKeysCommand(String keys) {
        JsonObject payload = new JsonObject();
        payload.addProperty("keys", keys);
        sendEnvelope(WsEnvelope.command(WsMessageType.SEND_KEYS, sessionId, payload));
    }

    private void sendMoveCursor(int pos) {
        JsonObject payload = new JsonObject();
        payload.addProperty("pos", pos);
        sendEnvelope(WsEnvelope.command(WsMessageType.MOVE_CURSOR, sessionId, payload));
    }

    private void sendAidCommand(int aidKey) {
        JsonObject payload = new JsonObject();
        payload.addProperty("aidKey", aidKey);
        sendEnvelope(WsEnvelope.command(WsMessageType.SEND_AID, sessionId, payload));
    }

    private CompletableFuture<WsEnvelope> sendEnvelope(WsEnvelope envelope) {
        return transport.send(envelope);
    }

    @Override
    public boolean moveCursor(int pos) {
        sendMoveCursor(pos);
        return true;
    }

    @Override
    public void sendAid(int aidKey) {
        JsonObject payload = new JsonObject();
        payload.addProperty("aidKey", aidKey);
        sendEnvelope(WsEnvelope.command(WsMessageType.SEND_AID, sessionId, payload));
    }

    @Override
    public void setUiHooks(SessionUiHooks hooks) {
        uiHooks = hooks == null ? HeadlessSessionUiHooks.INSTANCE : hooks;
    }

    @Override
    public SessionUiHooks getUiHooks() {
        return uiHooks;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T extension(Class<T> capability) {
        if (TerminalOps.class.equals(capability)) {
            return (T) terminalOps;
        }
        throw new UnsupportedCapabilityException(capability);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return null;
    }

    @Override
    public void onMessage(WsEnvelope envelope) {
        String type = envelope.getType();
        if (WsMessageType.SCREEN_REGION_UPDATED.equals(type)) {
            JsonObject payload = envelope.getPayload();
            Map<String, String> planes = WsMessageCodec.gson().fromJson(payload.get("planes"),
                    new TypeToken<Map<String, String>>() { }.getType());
            screenModel.applyRegion(
                    payload.get("inUpdate").getAsInt(),
                    payload.get("startRow").getAsInt(),
                    payload.get("startCol").getAsInt(),
                    payload.get("endRow").getAsInt(),
                    payload.get("endCol").getAsInt(),
                    payload.get("currentRow").getAsInt(),
                    payload.get("currentCol").getAsInt(),
                    payload.get("cursorActive").getAsBoolean(),
                    planes);
        } else if (WsMessageType.SCREEN_SIZE_CHANGED.equals(type)) {
            JsonObject payload = envelope.getPayload();
            screenModel.applySize(payload.get("rows").getAsInt(), payload.get("cols").getAsInt());
        } else if (WsMessageType.OIA_CHANGED.equals(type)) {
            JsonObject payload = envelope.getPayload();
            OiaStateDto oia = WsMessageCodec.gson().fromJson(payload.get("oia"), OiaStateDto.class);
            screenModel.getRemoteOiaModel().apply(oia);
            screenModel.getRemoteOiaModel().fireChanged(payload.get("change").getAsInt());
        } else if (WsMessageType.SESSION_STATE_CHANGED.equals(type)) {
            int state = envelope.getPayload().get("state").getAsInt();
            if (state == SessionStateConstants.STATE_CONNECTED) {
                refreshSnapshot();
            }
            org.tn5250j.session.api.SessionChangeEvent event =
                    new org.tn5250j.session.api.SessionChangeEvent(this);
            event.setState(state);
            for (SessionListener listener : new ArrayList<>(listeners)) {
                listener.onSessionChanged(event);
            }
        } else if (WsMessageType.BELL.equals(type)) {
            uiHooks.signalBell();
        } else if (WsMessageType.UI_PROMPT_REQUEST.equals(type)) {
            handleUiPrompt(envelope);
        }
    }

    private void handleUiPrompt(WsEnvelope envelope) {
        JsonObject payload = envelope.getPayload();
        String kind = payload.get("kind").getAsString();
        JsonObject replyPayload = new JsonObject();
        switch (kind) {
            case "systemRequest":
                replyPayload.add("value", WsMessageCodec.gson().toJsonTree(uiHooks.promptSystemRequest()));
                break;
            case "acceptCertificate":
                replyPayload.addProperty("accepted",
                        uiHooks.acceptUntrustedCertificate(payload.get("info").getAsString()));
                break;
            case "rememberCertificate":
                replyPayload.addProperty("accepted", uiHooks.rememberAcceptedCertificate());
                break;
            case "confirmSaveSettings":
                replyPayload.addProperty("accepted",
                        uiHooks.confirmSaveSettings(payload.get("message").getAsString()));
                break;
            default:
                replyPayload.addProperty("accepted", false);
        }
        transport.send(WsEnvelope.reply(envelope.getId(), replyPayload));
    }

    @Override
    public void onClosed(String reason) {
        connected = false;
    }

    @Override
    public void onError(Throwable error) {
        connected = false;
    }
}
