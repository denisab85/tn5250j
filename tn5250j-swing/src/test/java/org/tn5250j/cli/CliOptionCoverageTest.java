package org.tn5250j.cli;

import java.util.Properties;
import org.junit.Test;
import org.tn5250j.TN5250jConstants;
import static org.junit.Assert.*;

/** Verifies every historical CLI capability is available through POSIX-style options. */
public class CliOptionCoverageTest {
    @Test
    public void sessionFlagsHavePosixShortOrLongForms() {
        DesktopOptions options = DesktopOptions.parse(
                "ibmi", "-w", "-u", "--proxy-host", "proxy", "--proxy-port", "1080",
                "--ssl-type", "TLS", "-N", "TERM01", "-B", "-o", "-e", "-g", "-t",
                "-c37", "-p992", "-f", "display.props");
        assertEquals("ibmi", options.session.host);
        assertTrue(options.session.wide && options.session.proxy && options.session.enhanced);
        assertTrue(options.session.gui && options.session.nameFromSystem && options.session.heartbeat);
        assertTrue(options.session.newWindow);
        assertEquals("TERM01", options.session.deviceName);
        assertEquals("proxy", options.session.proxyHost);
        assertEquals(Integer.valueOf(1080), options.session.proxyPort);
        assertEquals("TLS", options.session.sslType);
        assertEquals(Integer.valueOf(992), options.session.port);
        assertEquals("37", options.session.codePage);
        assertEquals("display.props", options.session.config);
        assertTrue(DesktopOptions.parse("ibmi", "--device-name-from-hostname").session.deviceNameFromHostname);
    }

    @Test
    public void desktopFlagsHavePosixShortOrLongForms() {
        DesktopOptions options = DesktopOptions.parse(
                "-r", "ws://127.0.0.1:5250", "-T", "token",
                "-s", "Prod", "-n", "-d", "-L", "en_US", "-W1000", "-H700");
        assertTrue(options.newInstance && options.daemon);
        assertEquals("Prod", options.sessions.get(0));
        assertEquals("ws://127.0.0.1:5250", options.remote.toString());
        assertEquals("token", options.remoteToken);
        assertEquals("en_US", options.locale);
        assertEquals(Integer.valueOf(1000), options.width);
        assertEquals(Integer.valueOf(700), options.height);
    }

    @Test
    public void serverFlagsHavePosixShortOrLongForms() {
        DesktopOptions options = DesktopOptions.parse("-S", "-b127.0.0.1", "-P5251", "-k", "secret");
        assertTrue(options.server);
        assertEquals(5251, options.listener.port);
        assertEquals("127.0.0.1", options.listener.bind);
        assertEquals("secret", options.listener.token);
    }

    @Test
    public void proxyHostAndPortApplyWithoutExplicitProxyFlag() {
        DesktopOptions saved = DesktopOptions.parse("ibmi", "--proxy-host", "socks", "--proxy-port", "1080");
        Properties props = DesktopOptions.parse().propertiesFor(saved);
        assertEquals("socks", props.getProperty(TN5250jConstants.SESSION_PROXY_HOST));
        assertEquals("1080", props.getProperty(TN5250jConstants.SESSION_PROXY_PORT));
    }
}
