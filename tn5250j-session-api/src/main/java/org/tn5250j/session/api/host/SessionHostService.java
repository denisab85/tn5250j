package org.tn5250j.session.api.host;

import java.util.List;

public interface SessionHostService {

    String openSession(SessionOpenRequest request);

    void closeSession(String sessionId);

    List<SessionSummary> listSessions();

    SessionHandle attach(String sessionId);
}
