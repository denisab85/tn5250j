package org.tn5250j.session.api.transport;

import org.tn5250j.session.wire.WsEnvelope;

public interface SessionEventSink {

    void onMessage(WsEnvelope envelope);

    void onClosed(String reason);

    void onError(Throwable error);
}
