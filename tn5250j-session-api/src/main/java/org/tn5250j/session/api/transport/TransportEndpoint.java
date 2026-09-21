package org.tn5250j.session.api.transport;

public final class TransportEndpoint {

    public enum Kind {
        IN_PROCESS,
        WEBSOCKET
    }

    private final Kind kind;
    private final String url;
    private final String authToken;

    private TransportEndpoint(Kind kind, String url, String authToken) {
        this.kind = kind;
        this.url = url;
        this.authToken = authToken;
    }

    public static TransportEndpoint inProcess() {
        return new TransportEndpoint(Kind.IN_PROCESS, null, null);
    }

    public static TransportEndpoint webSocket(String url, String authToken) {
        return new TransportEndpoint(Kind.WEBSOCKET, url, authToken);
    }

    public Kind getKind() {
        return kind;
    }

    public String getUrl() {
        return url;
    }

    public String getAuthToken() {
        return authToken;
    }
}
