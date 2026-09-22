package org.tn5250j.cli;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import org.tn5250j.TN5250jConstants;
import org.tn5250j.session.server.ServerOptions.PortConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** Session settings shared by command-line launches and saved session definitions. */
public final class SessionOptions {
    @Parameters(index = "0", arity = "0..1", paramLabel = "HOST",
            description = "IBM i host; omit to choose a saved session.")
    public String host;
    @Option(names = {"-p", "--host-port"}, paramLabel = "PORT", converter = PortConverter.class,
            description = "IBM i TN5250 port (normally 23, or 992 for TLS).")
    public Integer port;
    @Option(names = {"-f", "--config"}, paramLabel = "FILE", description = "Session properties file.")
    public String config;
    @Option(names = {"-c", "--code-page"}, paramLabel = "CCSID", description = "Host code page.")
    public String codePage;
    @Option(names = {"-e", "--enhanced"}, description = "Enable enhanced TN5250 negotiation.")
    public boolean enhanced;
    @Option(names = {"-g", "--gui"}, description = "Enable graphical terminal enhancements.")
    public boolean gui;
    @Option(names = {"-t", "--name-from-system"}, description = "Use the configured session name in the title.")
    public boolean nameFromSystem;
    @Option(names = {"-w", "--wide"}, description = "Use a 27x132 screen (default: 24x80).")
    public boolean wide;
    @Option(names = {"-u", "--proxy"}, description = "Enable the SOCKS proxy.")
    public boolean proxy;
    @Option(names = {"--proxy-host"}, paramLabel = "HOST", description = "SOCKS proxy host.")
    public String proxyHost;
    @Option(names = {"--proxy-port"}, paramLabel = "PORT", converter = PortConverter.class,
            description = "SOCKS proxy port.")
    public Integer proxyPort;
    @Option(names = {"--ssl-type"}, paramLabel = "TYPE", description = "TLS/SSL implementation, e.g. TLS.")
    public String sslType;
    @Option(names = {"-N", "--device-name"}, paramLabel = "NAME", description = "Requested terminal device name.")
    public String deviceName;
    @Option(names = "--device-name-from-hostname", description = "Use this machine's hostname as device name.")
    public boolean deviceNameFromHostname;
    @Option(names = {"-B", "--heartbeat"}, description = "Enable session heartbeat.")
    public boolean heartbeat;
    @Option(names = {"-o", "--new-window"}, description = "Open each session in a separate window.")
    public boolean newWindow;

    /** Only explicitly supplied properties: suitable for overlaying launch settings on a saved session. */
    public Properties toProperties() {
        Properties props = new Properties();
        put(props, TN5250jConstants.SESSION_HOST, host);
        put(props, TN5250jConstants.SESSION_HOST_PORT, port);
        put(props, TN5250jConstants.SESSION_CODE_PAGE, codePage);
        if (enhanced) props.setProperty(TN5250jConstants.SESSION_TN_ENHANCED, "1");
        if (gui) props.setProperty(TN5250jConstants.SESSION_USE_GUI, "1");
        if (nameFromSystem) props.setProperty(TN5250jConstants.SESSION_TERM_NAME_SYSTEM, "1");
        if (wide) props.setProperty(TN5250jConstants.SESSION_SCREEN_SIZE, TN5250jConstants.SCREEN_SIZE_27X132_STR);
        put(props, TN5250jConstants.SESSION_PROXY_HOST, proxyHost);
        put(props, TN5250jConstants.SESSION_PROXY_PORT, proxyPort);
        put(props, TN5250jConstants.SSL_TYPE, sslType);
        put(props, TN5250jConstants.SESSION_DEVICE_NAME, deviceName);
        if (deviceNameFromHostname) {
            String hostname;
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException ex) {
                hostname = "UNKNOWN_HOST";
            }
            props.setProperty(TN5250jConstants.SESSION_DEVICE_NAME, hostname);
        }
        if (heartbeat) props.setProperty(TN5250jConstants.SESSION_HEART_BEAT, "1");
        return props;
    }

    private static void put(Properties props, String key, Object value) {
        if (value != null) props.setProperty(key, value.toString());
    }
}
