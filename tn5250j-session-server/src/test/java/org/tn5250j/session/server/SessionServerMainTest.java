package org.tn5250j.session.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.Test;
import picocli.CommandLine;
import static org.junit.Assert.*;

public class SessionServerMainTest {
    @Test
    public void helpReturnsWithoutOpeningAListener() {
        StringWriter output = new StringWriter();
        int exit = SessionServerMain.commandLine().setOut(new PrintWriter(output)).execute("--help");
        assertEquals(0, exit);
        assertTrue(output.toString().contains("--port"));
        assertTrue(output.toString().contains("--bind"));
    }

    @Test
    public void shortAndLongOptionsReachListenerSettings() {
        for (String[] args : new String[][] {
                {"--port=5251", "--bind=127.0.0.1", "--token=secret"},
                {"-P5251", "-b127.0.0.1", "-ksecret"},
                {"-p", "5251", "-b", "127.0.0.1", "-k", "secret"}
        }) {
            CommandLine command = SessionServerMain.commandLine();
            command.parseArgs(args);
            ServerOptions options = (ServerOptions) command.getMixins().get("options");
            assertEquals(5251, options.port);
            assertEquals("127.0.0.1", options.bind);
            assertEquals("secret", options.token);
        }
    }

    @Test
    public void invalidInputReturnsUsageExitCode() {
        for (String[] args : new String[][] {{"--unknown"}, {"--port"}, {"--port=nope"},
                {"--port=0"}, {"--port=65536"}, {"--bind"}, {"--token"}, {"extra"},
                {"-p5251", "--port=5252"}}) {
            StringWriter errors = new StringWriter();
            int exit = SessionServerMain.commandLine().setErr(new PrintWriter(errors)).execute(args);
            assertEquals(errors.toString(), 2, exit);
        }
    }
}
