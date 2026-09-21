package org.tn5250j.session.rpc;

import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RpcRegistry {

    private final Map<String, RpcMethod> methods = new ConcurrentHashMap<>();

    public void register(String method, RpcMethod handler) {
        methods.put(method, handler);
    }

    public JsonObject invoke(RpcContext context) throws Exception {
        RpcMethod method = methods.get(context.getMethod());
        if (method == null) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Unknown RPC method: " + context.getMethod());
            return error;
        }
        return method.invoke(context);
    }

    public boolean supports(String method) {
        return methods.containsKey(method);
    }
}
