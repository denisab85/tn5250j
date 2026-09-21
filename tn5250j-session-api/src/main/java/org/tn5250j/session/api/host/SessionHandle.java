package org.tn5250j.session.api.host;

import org.tn5250j.session.api.transport.SessionEventSink;

public interface SessionHandle extends AutoCloseable {

    String getSessionId();

    void attach(SessionEventSink sink);

    @Override
    void close();
}
