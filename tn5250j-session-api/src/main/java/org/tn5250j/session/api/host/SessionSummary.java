package org.tn5250j.session.api.host;

public final class SessionSummary {

    private final String sessionId;
    private final String sessionName;
    private final boolean connected;

    public SessionSummary(String sessionId, String sessionName, boolean connected) {
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.connected = connected;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public boolean isConnected() {
        return connected;
    }
}
