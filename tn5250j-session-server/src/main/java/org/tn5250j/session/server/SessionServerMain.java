package org.tn5250j.session.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.tn5250j.tools.logging.SessionDebugLog;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.OptionSpec;

@Command(name = "tn5250j-session-server", description = "Host TN5250 sessions over WebSocket.")
public final class SessionServerMain implements Callable<Integer> {

    @Mixin
    private ServerOptions options = new ServerOptions();

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true,
            description = "Show this help and exit.")
    private boolean help;

    public static CommandLine commandLine() {
        CommandLine command = new CommandLine(new SessionServerMain()).setExpandAtFiles(false);
        // The desktop reserves -p for IBM i; standalone mode can also use it for the listener.
        OptionSpec port = command.getCommandSpec().findOption("--port");
        command.getCommandSpec().remove(port);
        command.getCommandSpec().addOption(OptionSpec.builder(port).names("-p", "-P", "--port").build());
        return command;
    }

    public static void main(String[] args) {
        System.exit(commandLine().execute(args));
    }

    @Override
    public Integer call() throws Exception {
        run(options);
        return 0;
    }

    public static void run(ServerOptions options) throws Exception {
        if (options.debugPlanesFull) {
            SessionDebugLog.enableFullPlaneLogging();
        } else if (options.debug) {
            SessionDebugLog.enableDebugLogging();
        }
        InetSocketAddress address = new InetSocketAddress(options.bind, options.port);
        ensurePortAvailable(address);
        WebSocketSessionServer server = new WebSocketSessionServer(address, options.token);
        Thread serverThread = new Thread(server, "tn5250j-session-server");
        serverThread.setDaemon(false);
        serverThread.start();
        if (!server.awaitStartup(30, TimeUnit.SECONDS)) {
            serverThread.join(1_000);
            if (serverThread.isAlive()) {
                server.stop();
                serverThread.join(5_000);
            }
            throw bindFailure(address);
        }
        serverThread.join();
    }

    static void ensurePortAvailable(InetSocketAddress address) throws IOException {
        try (ServerSocket probe = new ServerSocket()) {
            probe.setReuseAddress(true);
            probe.bind(address);
        } catch (BindException ex) {
            throw bindFailure(address);
        }
    }

    private static BindException bindFailure(InetSocketAddress address) {
        int port = address.getPort();
        BindException failure = new BindException(
                "Port " + port + " on " + address.getHostString() + " is already in use. "
                        + "Stop the other listener (ss -tlnp | grep " + port
                        + " or lsof -i :" + port + ") or choose another --port.");
        return failure;
    }
}
