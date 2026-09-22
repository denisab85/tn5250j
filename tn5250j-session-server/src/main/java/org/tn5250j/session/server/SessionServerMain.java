package org.tn5250j.session.server;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
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
        WebSocketSessionServer server = new WebSocketSessionServer(
                new InetSocketAddress(options.bind, options.port), options.token);
        server.start();
        System.out.println("tn5250j session server listening on ws://" + options.bind + ":" + options.port);
        Thread.currentThread().join();
    }
}
