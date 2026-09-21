package org.tn5250j.session.client.local;

import org.junit.Test;
import org.tn5250j.Session5250;
import org.tn5250j.SessionConfig;
import org.tn5250j.session.api.ScreenModel;
import org.tn5250j.session.api.SessionClient;
import org.tn5250j.session.api.TerminalOps;
import org.tn5250j.session.client.SessionClientFactory;

import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LocalSessionClientTest {

    @Test
    public void wrapLocalExposesScreenModelAndTerminalOps() {
        Properties props = new Properties();
        props.setProperty("SESSION_HOST", "example");
        SessionConfig config = new SessionConfig("", "test");
        Session5250 session = new Session5250(props, "", "test", config);

        SessionClient client = SessionClientFactory.wrapLocal(session);

        ScreenModel screen = client.getScreen();
        assertNotNull(screen);
        assertSame(session.getScreen(), ((LocalScreenModel) screen).getScreen5250());
        assertNotNull(client.extension(TerminalOps.class));
    }
}
