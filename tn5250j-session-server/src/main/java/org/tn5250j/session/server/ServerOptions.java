package org.tn5250j.session.server;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.TypeConversionException;

/** Shared listener options for the desktop launcher and standalone gateway. */
public final class ServerOptions {
    @Option(names = {"-b", "--bind"}, paramLabel = "ADDRESS",
            description = "Server listen address (default: ${DEFAULT-VALUE}).")
    public String bind = "127.0.0.1";

    @Option(names = {"-P", "--port"}, paramLabel = "PORT", converter = PortConverter.class,
            description = "Server WebSocket port (default: ${DEFAULT-VALUE}).")
    public int port = 5250;

    @Option(names = {"-k", "--token"}, paramLabel = "TOKEN",
            description = "Require this token from remote clients.")
    public String token = "";

    public static final class PortConverter implements ITypeConverter<Integer> {
        @Override
        public Integer convert(String value) {
            try {
                int port = Integer.parseInt(value);
                if (port >= 1 && port <= 65535) return port;
            } catch (NumberFormatException ignored) {
                // Report the same error for nonnumeric and out-of-range input.
            }
            throw new TypeConversionException("port must be an integer between 1 and 65535");
        }
    }
}
