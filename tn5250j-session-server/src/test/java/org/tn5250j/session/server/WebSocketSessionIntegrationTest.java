package org.tn5250j.session.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tn5250j.session.api.ConnectionProfile;
import org.tn5250j.session.client.remote.RemoteSessionClient;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WebSocketSessionIntegrationTest {

    private WebSocketSessionServer server;
    private int port;

    @Before
    public void setUp() throws Exception {
        port = findFreePort();
        server = new WebSocketSessionServer(new InetSocketAddress("127.0.0.1", port), "");
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void remoteClientCanOpenSessionAndReceiveSnapshot() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("SESSION_HOST", "127.0.0.1");
        ConnectionProfile profile = ConnectionProfile.remote(
                "ws://127.0.0.1:" + port, "", "integration-test", props);
        RemoteSessionClient client = new RemoteSessionClient(profile);
        client.open();

        assertNotNull(client.getScreen());
        assertTrue(client.getScreen().getRows() > 0);
        assertTrue(client.getScreen().getColumns() > 0);
    }

    private static int findFreePort() throws Exception {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
