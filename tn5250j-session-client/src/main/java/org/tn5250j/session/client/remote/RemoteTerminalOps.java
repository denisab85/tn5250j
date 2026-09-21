package org.tn5250j.session.client.remote;

import com.google.gson.JsonObject;
import org.tn5250j.session.api.TerminalOps;
import org.tn5250j.session.wire.WsEnvelope;
import org.tn5250j.session.wire.WsMessageType;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

final class RemoteTerminalOps implements TerminalOps {

    private String sessionId;
    private final Function<WsEnvelope, CompletableFuture<WsEnvelope>> sender;

    RemoteTerminalOps(String sessionId, Function<WsEnvelope, CompletableFuture<WsEnvelope>> sender) {
        this.sessionId = sessionId;
        this.sender = sender;
    }

    void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void systemRequest(char code) {
        invoke("terminal.systemRequest", codeString(code));
    }

    @Override
    public void systemRequest(String code) {
        invoke("terminal.systemRequest", code);
    }

    @Override
    public void toggleDebug() {
        invoke("terminal.toggleDebug", null);
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    private void invoke(String method, String code) {
        JsonObject payload = new JsonObject();
        payload.addProperty("method", method);
        JsonObject args = new JsonObject();
        if (code != null) {
            args.addProperty("code", code);
        }
        payload.add("args", args);
        WsEnvelope rpc = WsEnvelope.command(WsMessageType.RPC, sessionId, payload);
        sender.apply(rpc);
    }

    private static String codeString(char code) {
        return String.valueOf(code);
    }
}
