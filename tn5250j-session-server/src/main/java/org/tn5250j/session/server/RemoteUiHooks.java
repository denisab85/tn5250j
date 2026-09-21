package org.tn5250j.session.server;

import com.google.gson.JsonObject;
import org.tn5250j.interfaces.SessionUiHooks;
import org.tn5250j.session.wire.WsEnvelope;
import org.tn5250j.session.wire.WsMessageType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

final class RemoteUiHooks implements SessionUiHooks {

    private static final long PROMPT_TIMEOUT_MS = 120_000L;

    private final String sessionId;
    private final Function<WsEnvelope, CompletableFuture<WsEnvelope>> sender;

    RemoteUiHooks(String sessionId, Function<WsEnvelope, CompletableFuture<WsEnvelope>> sender) {
        this.sessionId = sessionId;
        this.sender = sender;
    }

    @Override
    public void signalBell() {
        JsonObject payload = new JsonObject();
        WsEnvelope event = WsEnvelope.event(WsMessageType.BELL, sessionId, payload);
        sender.apply(event);
    }

    @Override
    public String promptSystemRequest() {
        JsonObject payload = new JsonObject();
        payload.addProperty("kind", "systemRequest");
        return promptString(payload);
    }

    @Override
    public boolean acceptUntrustedCertificate(String info) {
        JsonObject payload = new JsonObject();
        payload.addProperty("kind", "acceptCertificate");
        payload.addProperty("info", info);
        return promptBoolean(payload, false);
    }

    @Override
    public boolean rememberAcceptedCertificate() {
        JsonObject payload = new JsonObject();
        payload.addProperty("kind", "rememberCertificate");
        return promptBoolean(payload, false);
    }

    @Override
    public boolean confirmSaveSettings(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("kind", "confirmSaveSettings");
        payload.addProperty("message", message);
        return promptBoolean(payload, false);
    }

    private String promptString(JsonObject payload) {
        WsEnvelope request = WsEnvelope.command(WsMessageType.UI_PROMPT_REQUEST, sessionId, payload);
        try {
            WsEnvelope reply = sender.apply(request).get(PROMPT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (reply.getPayload() != null && reply.getPayload().has("value")) {
                if (reply.getPayload().get("value").isJsonNull()) {
                    return null;
                }
                return reply.getPayload().get("value").getAsString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean promptBoolean(JsonObject payload, boolean defaultValue) {
        WsEnvelope request = WsEnvelope.command(WsMessageType.UI_PROMPT_REQUEST, sessionId, payload);
        try {
            WsEnvelope reply = sender.apply(request).get(PROMPT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (reply.getPayload() != null && reply.getPayload().has("accepted")) {
                return reply.getPayload().get("accepted").getAsBoolean();
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }
}
