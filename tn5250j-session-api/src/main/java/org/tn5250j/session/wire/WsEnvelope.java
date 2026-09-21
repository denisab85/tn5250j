package org.tn5250j.session.wire;

import com.google.gson.JsonObject;

public final class WsEnvelope {

    public static final int PROTOCOL_VERSION = 1;

    private int v = PROTOCOL_VERSION;
    private String type;
    private String id;
    private String correlationId;
    private String sessionId;
    private JsonObject payload;

    public WsEnvelope() {
    }

    public WsEnvelope(String type, String id) {
        this.type = type;
        this.id = id;
    }

    public static WsEnvelope event(String type, String sessionId, JsonObject payload) {
        WsEnvelope envelope = new WsEnvelope(type, java.util.UUID.randomUUID().toString());
        envelope.sessionId = sessionId;
        envelope.payload = payload;
        return envelope;
    }

    public static WsEnvelope command(String type, String sessionId, JsonObject payload) {
        WsEnvelope envelope = new WsEnvelope(type, java.util.UUID.randomUUID().toString());
        envelope.sessionId = sessionId;
        envelope.payload = payload;
        return envelope;
    }

    public static WsEnvelope reply(String correlationId, JsonObject payload) {
        WsEnvelope envelope = new WsEnvelope(WsMessageType.REPLY, java.util.UUID.randomUUID().toString());
        envelope.correlationId = correlationId;
        envelope.payload = payload;
        return envelope;
    }

    public static WsEnvelope error(String correlationId, String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message);
        WsEnvelope envelope = new WsEnvelope(WsMessageType.ERROR, java.util.UUID.randomUUID().toString());
        envelope.correlationId = correlationId;
        envelope.payload = payload;
        return envelope;
    }

    public int getV() {
        return v;
    }

    public void setV(int v) {
        this.v = v;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public JsonObject getPayload() {
        return payload;
    }

    public void setPayload(JsonObject payload) {
        this.payload = payload;
    }
}
