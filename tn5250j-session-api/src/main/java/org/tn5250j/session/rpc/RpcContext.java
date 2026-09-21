package org.tn5250j.session.rpc;

import com.google.gson.JsonObject;

public final class RpcContext {

    private final String sessionId;
    private final String method;
    private final JsonObject args;

    public RpcContext(String sessionId, String method, JsonObject args) {
        this.sessionId = sessionId;
        this.method = method;
        this.args = args;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getMethod() {
        return method;
    }

    public JsonObject getArgs() {
        return args;
    }
}
