package org.tn5250j.session.client;

import org.tn5250j.Session5250;
import org.tn5250j.framework.common.SessionManager;
import org.tn5250j.session.api.ConnectionProfile;
import org.tn5250j.session.api.SessionClient;
import org.tn5250j.session.client.local.LocalSessionClient;
import org.tn5250j.session.client.remote.RemoteSessionClient;

public final class SessionClientFactory {

    private SessionClientFactory() {
    }

    public static SessionClient create(ConnectionProfile profile) {
        if (profile.getMode() == ConnectionProfile.Mode.IN_PROCESS) {
            Session5250 session = SessionManager.instance().openSession(
                    profile.getSessionProperties(),
                    profile.getConfigurationResource(),
                    profile.getSessionName());
            return new LocalSessionClient(session);
        }
        return new RemoteSessionClient(profile);
    }

    public static SessionClient wrapLocal(Session5250 session) {
        return new LocalSessionClient(session);
    }
}
