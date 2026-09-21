package org.tn5250j.session.rpc;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface RpcMethod {

    JsonObject invoke(RpcContext context) throws Exception;
}
