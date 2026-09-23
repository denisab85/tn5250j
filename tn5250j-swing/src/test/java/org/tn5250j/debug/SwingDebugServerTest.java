package org.tn5250j.debug;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SwingDebugServerTest {

    private SwingDebugServer server;

    @Before
    public void setUp() throws IOException {
        DebugInterfaceOptions options = new DebugInterfaceOptions();
        options.enabled = true;
        options.bind = "127.0.0.1";
        options.port = 0;
        options.token = "secret";
        server = new SwingDebugServer(options);
        server.start(options);
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void healthRequiresTokenWhenConfigured() throws IOException {
        int status = request("GET", "/health", null, null);
        assertEquals(401, status);

        status = request("GET", "/health", "Bearer secret", null);
        assertEquals(200, status);
    }

    @Test
    public void listSessionsReturnsJsonArray() throws IOException {
        String body = requestBody("GET", "/v1/sessions", "Bearer secret", null);
        assertTrue(body.startsWith("["));
    }

    private int request(String method, String path, String authorization, String payload) throws IOException {
        HttpURLConnection connection = open(method, path, authorization);
        if (payload != null) {
            connection.setDoOutput(true);
            connection.getOutputStream().write(payload.getBytes(StandardCharsets.UTF_8));
        }
        return connection.getResponseCode();
    }

    private String requestBody(String method, String path, String authorization, String payload) throws IOException {
        HttpURLConnection connection = open(method, path, authorization);
        if (payload != null) {
            connection.setDoOutput(true);
            connection.getOutputStream().write(payload.getBytes(StandardCharsets.UTF_8));
        }
        InputStream stream = connection.getResponseCode() >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();
        return readStream(stream);
    }

    private HttpURLConnection open(String method, String path, String authorization) throws IOException {
        URL url = new URL("http://127.0.0.1:" + serverPort() + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(2000);
        if (authorization != null) {
            connection.setRequestProperty("Authorization", authorization);
        }
        return connection;
    }

    private int serverPort() {
        return server.getPort();
    }

    private static String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder body = new StringBuilder();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            body.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        }
        return body.toString();
    }
}
