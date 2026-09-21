package org.tn5250j.session.server;

import java.net.InetSocketAddress;

public final class SessionServerMain {

    public static void main(String[] args) throws Exception {
        String bind = "127.0.0.1";
        int port = 5250;
        String token = "";

        for (int i = 0; i < args.length; i++) {
            if ("--bind".equals(args[i]) && i + 1 < args.length) {
                bind = args[++i];
            } else if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if ("--token".equals(args[i]) && i + 1 < args.length) {
                token = args[++i];
            }
        }

        WebSocketSessionServer server = new WebSocketSessionServer(new InetSocketAddress(bind, port), token);
        server.start();
        System.out.println("tn5250j session server listening on ws://" + bind + ":" + port);
        Thread.currentThread().join();
    }
}
