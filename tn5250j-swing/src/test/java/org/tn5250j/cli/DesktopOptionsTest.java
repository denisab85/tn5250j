package org.tn5250j.cli;

import java.util.Arrays;
import java.util.Properties;
import org.junit.Test;
import org.tn5250j.TN5250jConstants;
import org.tn5250j.session.api.ConnectionProfile;
import picocli.CommandLine.ParameterException;
import static org.junit.Assert.*;

public class DesktopOptionsTest {
    @Test
    public void canonicalSessionSettingsParseCorrectly() {
        DesktopOptions options = DesktopOptions.parse(StoredArguments.split(
                "--host-port=992 --config display.props --code-page 37 --enhanced --gui --name-from-system --wide --proxy --proxy-host proxy --proxy-port 1080 --ssl-type TLS --device-name TERM01 --heartbeat --new-window --new-instance --daemon --width 1000 --height 700 ibmi"));
        assertEquals("992", options.session.port.toString());
        assertEquals("display.props", options.session.config);
        assertEquals("37", options.session.codePage);
        assertTrue(options.session.enhanced && options.session.gui && options.session.nameFromSystem);
        assertTrue(options.session.wide && options.session.proxy && options.session.heartbeat && options.session.newWindow);
        assertEquals("proxy", options.session.proxyHost);
        assertEquals(Integer.valueOf(1080), options.session.proxyPort);
        assertEquals("TLS", options.session.sslType);
        assertEquals("TERM01", options.session.deviceName);
        assertTrue(options.newInstance && options.daemon);
        assertEquals(Integer.valueOf(1000), options.width);
        assertEquals(Integer.valueOf(700), options.height);
        assertEquals("ibmi", options.session.host);
    }

    @Test
    public void shortOptionsClustersAttachedValuesAndRepeatedSessionsWork() {
        DesktopOptions options = DesktopOptions.parse("-neg", "-c37", "-p992", "-W1000", "-H700", "-s", "foo 1", "--session=bar", "-L", "fr_CA");
        assertTrue(options.newInstance && options.session.enhanced && options.session.gui);
        assertEquals(Integer.valueOf(992), options.session.port);
        assertEquals("37", options.session.codePage);
        assertEquals(Arrays.asList("foo 1", "bar"), options.sessions);
        assertEquals("fr_CA", options.locale);
    }

    @Test
    public void remoteLaunchSurvivesSavedSessionResolution() {
        DesktopOptions launch = DesktopOptions.parse("--remote=ws://127.0.0.1:5250", "--remote-token", "secret", "-s", "Production", "-p992");
        DesktopOptions saved = DesktopOptions.parse("ibmi", "-p", "23", "--code-page", "37", "--wide");
        ConnectionProfile profile = launch.profileFor("Production", saved);
        assertEquals(ConnectionProfile.Mode.REMOTE, profile.getMode());
        assertEquals("ws://127.0.0.1:5250", profile.getRemoteUrl());
        assertEquals("secret", profile.getAuthToken());
        assertEquals("ibmi", profile.getSessionProperties().getProperty(TN5250jConstants.SESSION_HOST));
        assertEquals("992", profile.getSessionProperties().getProperty(TN5250jConstants.SESSION_HOST_PORT));
        assertEquals("37", profile.getSessionProperties().getProperty(TN5250jConstants.SESSION_CODE_PAGE));
        assertEquals(TN5250jConstants.SCREEN_SIZE_27X132_STR, profile.getSessionProperties().getProperty(TN5250jConstants.SESSION_SCREEN_SIZE));
    }

    @Test
    public void remoteLaunchAppliesToDialogSelectionsAndSavedEndpoints() {
        DesktopOptions saved = DesktopOptions.parse("ibmi", "--remote", "ws://old:5250", "--remote-token", "old-token");
        DesktopOptions launch = DesktopOptions.parse("-r", "ws://localhost:5250");
        assertEquals("", launch.profileFor("Chosen in dialog", saved).getAuthToken());
        assertEquals("ws://localhost:5250", launch.profileFor("Chosen in dialog", saved).getRemoteUrl());
        assertEquals("old-token", DesktopOptions.parse().profileFor("Saved", saved).getAuthToken());
        assertNull(DesktopOptions.parse().profileFor("Local", DesktopOptions.parse("ibmi")));
    }

    @Test
    public void launchOverridesSavedProxySettingsWithoutLosingOtherFields() {
        DesktopOptions saved = DesktopOptions.parse("ibmi", "--proxy", "--proxy-host", "old-proxy", "--proxy-port", "1080");
        Properties props = DesktopOptions.parse("--proxy-host=new-proxy").propertiesFor(saved);
        assertEquals("new-proxy", props.getProperty(TN5250jConstants.SESSION_PROXY_HOST));
        assertEquals("1080", props.getProperty(TN5250jConstants.SESSION_PROXY_PORT));
        assertEquals(TN5250jConstants.SCREEN_SIZE_24X80_STR, props.getProperty(TN5250jConstants.SESSION_SCREEN_SIZE));
    }

    @Test
    public void deviceNameOptionsAreDistinct() {
        assertTrue(DesktopOptions.parse("ibmi", "--device-name-from-hostname").session.deviceNameFromHostname);
        DesktopOptions literal = DesktopOptions.parse("ibmi", "--device-name=hostname");
        assertFalse(literal.session.deviceNameFromHostname);
        assertEquals("hostname", literal.session.deviceName);
    }

    @Test
    public void separatorAndLiteralAtSignsWork() {
        assertEquals("-ibmi", DesktopOptions.parse("--", "-ibmi").session.host);
        assertEquals("@settings", DesktopOptions.parse("-f", "@settings", "ibmi").session.config);
        assertEquals("ibmi", DesktopOptions.parse(new String[]{"ibmi", null, null}).session.host);
    }

    @Test
    public void serverOptionsAreDistinctFromHostOptions() {
        DesktopOptions options = DesktopOptions.parse("-S", "-b127.0.0.1", "-P5251", "-k", "secret");
        assertTrue(options.server);
        assertEquals(5251, options.listener.port);
        assertEquals("127.0.0.1", options.listener.bind);
        assertEquals("secret", options.listener.token);
    }

    @Test
    public void rejectsMalformedAndConflictingArguments() {
        for (String[] args : new String[][] {
                {"--unknown"}, {"--remote"}, {"-s"}, {"-s", "-s", "other"},
                {"--host-port=nope"}, {"--host-port=0"}, {"--host-port=65536"},
                {"--server", "--port=-1"}, {"--width=0"}, {"--height=-1"},
                {"--remote=http://localhost"}, {"--remote=ws:///missing-host"}, {"--remote-token=secret"},
                {"--server", "--remote=ws://localhost"}, {"--port=5250"}, {"--server", "-p23"},
                {"ibmi", "--session=Prod"}, {"one", "two"}, {"--device-name=TERM", "--device-name-from-hostname"},
                {"--width=10", "--width=20"}
        }) {
            try {
                DesktopOptions.parse(args);
                fail("Expected usage error for " + Arrays.toString(args));
            } catch (ParameterException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }
}
