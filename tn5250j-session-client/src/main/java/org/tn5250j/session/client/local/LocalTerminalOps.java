package org.tn5250j.session.client.local;

import org.tn5250j.Session5250;
import org.tn5250j.framework.tn5250.tnvt;
import org.tn5250j.session.api.TerminalOps;

public final class LocalTerminalOps implements TerminalOps {

    private final Session5250 session;

    public LocalTerminalOps(Session5250 session) {
        this.session = session;
    }

    @Override
    public void systemRequest(char code) {
        tnvt vt = session.getVT();
        if (vt != null) {
            vt.systemRequest(code);
        }
    }

    @Override
    public void systemRequest(String code) {
        tnvt vt = session.getVT();
        if (vt != null) {
            vt.systemRequest(code);
        }
    }

    @Override
    public void toggleDebug() {
        tnvt vt = session.getVT();
        if (vt != null) {
            vt.toggleDebug();
        }
    }

    @Override
    public boolean isSupported() {
        return session.getVT() != null;
    }
}
