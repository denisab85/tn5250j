package org.tn5250j.session.server;

import org.tn5250j.Session5250;
import org.tn5250j.framework.common.SessionManager;
import org.tn5250j.session.api.host.SessionHandle;
import org.tn5250j.session.api.host.SessionHostService;
import org.tn5250j.session.api.host.SessionOpenRequest;
import org.tn5250j.session.api.host.SessionSummary;
import org.tn5250j.session.api.transport.SessionEventSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.tn5250j.session.wire.WsEnvelope;

public final class DefaultSessionHostService implements SessionHostService {

    private final Map<String, SessionBridge> bridges = new ConcurrentHashMap<>();

    @Override
    public synchronized String openSession(SessionOpenRequest request) {
        Properties props = new Properties();
        props.putAll(request.getProperties());
        Session5250 session = SessionManager.instance().openSession(
                props,
                request.getConfigurationResource(),
                request.getSessionName());
        String sessionId = UUID.randomUUID().toString();
        bridges.put(sessionId, new SessionBridge(sessionId, session));
        return sessionId;
    }

    @Override
    public synchronized void closeSession(String sessionId) {
        SessionBridge bridge = bridges.remove(sessionId);
        if (bridge != null) {
            bridge.close();
        }
    }

    @Override
    public synchronized List<SessionSummary> listSessions() {
        List<SessionSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, SessionBridge> entry : bridges.entrySet()) {
            Session5250 session = entry.getValue().getSession();
            summaries.add(new SessionSummary(entry.getKey(), session.getSessionName(),
                    session.isConnected()));
        }
        return summaries;
    }

    @Override
    public synchronized SessionHandle attach(String sessionId) {
        SessionBridge bridge = bridges.get(sessionId);
        if (bridge == null) {
            throw new IllegalArgumentException("Unknown session: " + sessionId);
        }
        return new BridgeSessionHandle(bridge);
    }

    public synchronized SessionBridge getBridge(String sessionId) {
        return bridges.get(sessionId);
    }

    private static final class BridgeSessionHandle implements SessionHandle {

        private final SessionBridge bridge;

        private BridgeSessionHandle(SessionBridge bridge) {
            this.bridge = bridge;
        }

        @Override
        public String getSessionId() {
            return bridge.getSessionId();
        }

        @Override
        public void attach(SessionEventSink sink) {
            bridge.bind(sink, unsupportedPromptSender());
        }

        private Function<WsEnvelope, java.util.concurrent.CompletableFuture<WsEnvelope>> unsupportedPromptSender() {
            return envelope -> java.util.concurrent.CompletableFuture.completedFuture(
                    WsEnvelope.error(envelope.getId(), "No prompt transport attached"));
        }

        @Override
        public void close() {
            bridge.close();
        }
    }
}
