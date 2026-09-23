package org.tn5250j.debug;

import org.tn5250j.session.server.ServerOptions;
import picocli.CommandLine.Option;

/** Local HTTP debug interface for the Swing desktop (screen state and input). */
public final class DebugInterfaceOptions {
    @Option(names = {"-A", "--debug-interface"},
            description = "Expose a localhost HTTP debug API for screen state and input.")
    public boolean enabled;

    @Option(names = {"--debug-bind"}, paramLabel = "ADDRESS",
            description = "Debug API listen address (default: ${DEFAULT-VALUE}).")
    public String bind = "127.0.0.1";

    @Option(names = {"--debug-port"}, paramLabel = "PORT", converter = ServerOptions.PortConverter.class,
            description = "Debug API listen port (default: ${DEFAULT-VALUE}).")
    public int port = 5036;

    @Option(names = {"--debug-token"}, paramLabel = "TOKEN",
            description = "Require this token for debug API requests.")
    public String token = "";
}
