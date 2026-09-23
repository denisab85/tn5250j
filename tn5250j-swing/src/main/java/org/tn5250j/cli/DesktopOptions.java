package org.tn5250j.cli;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.tn5250j.TN5250jConstants;
import org.tn5250j.session.api.ConnectionProfile;
import org.tn5250j.debug.DebugInterfaceOptions;
import org.tn5250j.session.server.ServerOptions;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(name = "tn5250j", description = "TN5250J desktop and session server.", abbreviateSynopsis = true,
        footer = "Long options require two dashes; short options use one letter.")
public final class DesktopOptions {
    @Spec private CommandSpec spec;
    @Mixin public SessionOptions session = new SessionOptions();
    @Mixin public ServerOptions listener = new ServerOptions();
    @Mixin public DebugInterfaceOptions debugInterface = new DebugInterfaceOptions();
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help and exit.")
    public boolean help;
    @Option(names = {"-S", "--server"}, description = "Run the session server without Swing.")
    public boolean server;
    @Option(names = {"-r", "--remote"}, paramLabel = "URL", description = "Connect Swing to a ws:// or wss:// session server.")
    public URI remote;
    @Option(names = {"-T", "--remote-token"}, paramLabel = "TOKEN", description = "Authenticate to the remote session server.")
    public String remoteToken;
    @Option(names = {"-s", "--session"}, paramLabel = "NAME", description = "Open a saved session (repeatable).")
    public List<String> sessions = new ArrayList<>();
    @Option(names = {"-n", "--new-instance"}, description = "Do not forward to an existing desktop instance.")
    public boolean newInstance;
    @Option(names = {"-d", "--daemon"}, description = "Accept desktop launch requests on localhost:3036.")
    public boolean daemon;
    @Option(names = {"-L", "--locale"}, paramLabel = "LOCALE", description = "UI locale, e.g. en_US.")
    public String locale;
    @Option(names = {"-W", "--width"}, paramLabel = "PIXELS", description = "Initial window width.")
    public Integer width;
    @Option(names = {"-H", "--height"}, paramLabel = "PIXELS", description = "Initial window height.")
    public Integer height;

    public CommandLine commandLine() {
        return new CommandLine(this).setExpandAtFiles(false);
    }

    public static DesktopOptions parse(String... args) {
        DesktopOptions options = new DesktopOptions();
        options.commandLine().parseArgs(StoredArguments.normalizeLegacy(nonNullArguments(args)));
        options.validate();
        return options;
    }

    private static String[] nonNullArguments(String[] args) {
        if (args == null) return new String[0];
        List<String> result = new ArrayList<>();
        for (String arg : args) {
            if (arg == null) continue;
            result.add(arg);
        }
        return result.toArray(new String[0]);
    }

    public void validate() {
        if (help) return;
        if (width != null && width <= 0 || height != null && height <= 0)
            fail("Window width and height must be positive.");
        if (remote != null && (!("ws".equalsIgnoreCase(remote.getScheme())
                || "wss".equalsIgnoreCase(remote.getScheme())) || remote.getHost() == null))
            fail("--remote requires a ws:// or wss:// URL with a host.");
        if (session.deviceName != null && session.deviceNameFromHostname)
            fail("Choose --device-name or --device-name-from-hostname, not both.");
        if (session.host != null && !sessions.isEmpty())
            fail("Choose HOST or --session, not both.");
        if (remoteToken != null && remote == null)
            fail("--remote-token requires --remote.");
        if (server) {
            for (CommandLine.Model.OptionSpec option : spec.commandLine().getParseResult().matchedOptions()) {
                if (!Arrays.asList("--server", "--bind", "--port", "--token", "--debug", "--debug-planes-full").stream()
                        .anyMatch(name -> Arrays.asList(option.names()).contains(name)))
                    fail("--server accepts only --bind, --port, --token, --debug and --debug-planes-full.");
            }
            if (session.host != null) fail("--server does not accept an IBM i host.");
        } else if (spec.commandLine().getParseResult().hasMatchedOption("--bind")
                || spec.commandLine().getParseResult().hasMatchedOption("--port")
                || spec.commandLine().getParseResult().hasMatchedOption("--token")) {
            fail("--bind, --port and --token require --server; use --host-port for IBM i.");
        }
        if (server && debugInterface.enabled) {
            fail("--debug-interface cannot be combined with --server.");
        }
    }

    private void fail(String message) {
        throw new ParameterException(spec.commandLine(), message);
    }

    public Properties propertiesFor(DesktopOptions saved) {
        Properties props = saved.session.toProperties();
        props.putAll(session.toProperties());
        String proxyHost = session.proxyHost != null ? session.proxyHost : saved.session.proxyHost;
        Integer proxyPort = session.proxyPort != null ? session.proxyPort : saved.session.proxyPort;
        if (proxyHost != null) props.setProperty(TN5250jConstants.SESSION_PROXY_HOST, proxyHost);
        if (proxyPort != null) props.setProperty(TN5250jConstants.SESSION_PROXY_PORT, proxyPort.toString());
        if (!props.containsKey(TN5250jConstants.SESSION_SCREEN_SIZE))
            props.setProperty(TN5250jConstants.SESSION_SCREEN_SIZE, TN5250jConstants.SCREEN_SIZE_24X80_STR);
        return props;
    }

    public ConnectionProfile profileFor(String name, DesktopOptions saved) {
        Properties props = propertiesFor(saved);
        java.util.Map<String, String> values = new java.util.HashMap<>();
        for (String key : props.stringPropertyNames()) values.put(key, props.getProperty(key));
        URI endpoint = remote != null ? remote : saved.remote;
        String token = remote != null ? remoteToken : saved.remoteToken;
        return endpoint == null ? null : ConnectionProfile.remote(endpoint.toString(), token == null ? "" : token, name, values);
    }
}
