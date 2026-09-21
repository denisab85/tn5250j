package org.tn5250j.session.wire;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public final class WsMessageCodec {

    private static final Gson GSON = new GsonBuilder().create();

    private WsMessageCodec() {
    }

    public static String encode(WsEnvelope envelope) {
        return GSON.toJson(envelope);
    }

    public static WsEnvelope decode(String json) {
        return GSON.fromJson(json, WsEnvelope.class);
    }

    public static JsonObject object(String json) {
        return GSON.fromJson(json, JsonObject.class);
    }

    public static Gson gson() {
        return GSON;
    }
}
