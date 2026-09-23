package org.tn5250j.debug;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.tn5250j.tools.logging.TN5250jLogFactory;
import org.tn5250j.tools.logging.TN5250jLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Localhost HTTP API for agents to inspect and drive the Swing desktop.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /health}</li>
 *   <li>{@code GET /v1/sessions}</li>
 *   <li>{@code GET /v1/sessions/{id}/screen} — logical render + painted Swing raster; {@code ?include=planes,raster}</li>
 *   <li>{@code POST /v1/sessions/{id}/input/keys} body {@code {"keys":"[enter]"}}</li>
 *   <li>{@code POST /v1/sessions/{id}/input/text} body {@code {"text":"HELLO"}}</li>
 *   <li>{@code POST /v1/sessions/{id}/input/mouse} body {@code {"row":12,"col":5,"clicks":1}}</li>
 * </ul>
 */
public final class SwingDebugServer {

    private static final Pattern SESSION_SCREEN = Pattern.compile("^/v1/sessions/(\\d+)/screen$");
    private static final Pattern SESSION_KEYS = Pattern.compile("^/v1/sessions/(\\d+)/input/keys$");
    private static final Pattern SESSION_TEXT = Pattern.compile("^/v1/sessions/(\\d+)/input/text$");
    private static final Pattern SESSION_MOUSE = Pattern.compile("^/v1/sessions/(\\d+)/input/mouse$");

    private static final TN5250jLogger LOG = TN5250jLogFactory.getLogger(SwingDebugServer.class);
    private static final Gson GSON = new Gson();

    private final SwingDebugService service = new SwingDebugService();
    private final String authToken;
    private HttpServer server;

    public SwingDebugServer(DebugInterfaceOptions options) {
        this.authToken = options.token == null ? "" : options.token;
    }

    public void start(DebugInterfaceOptions options) throws IOException {
        if (server != null) {
            return;
        }
        server = HttpServer.create(new InetSocketAddress(options.bind, options.port), 0);
        server.createContext("/health", this::handleHealth);
        server.createContext("/v1/sessions", this::handleSessions);
        server.createContext("/", this::handleRoot);
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "swing-debug-http");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        LOG.info("Swing debug interface listening on http://" + options.bind + ":" + options.port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public int getPort() {
        return server == null ? -1 : server.getAddress().getPort();
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, error("Method not allowed"));
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("name", "tn5250j-swing-debug");
        payload.addProperty("version", 1);
        payload.addProperty("health", "/health");
        payload.addProperty("sessions", "/v1/sessions");
        sendJson(exchange, 200, payload);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("ok", true);
        sendJson(exchange, 200, payload);
    }

    private void handleSessions(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        Matcher screen = SESSION_SCREEN.matcher(path);
        if (screen.matches() && "GET".equalsIgnoreCase(method)) {
            handleScreen(exchange, Integer.parseInt(screen.group(1)));
            return;
        }
        Matcher keys = SESSION_KEYS.matcher(path);
        if (keys.matches() && "POST".equalsIgnoreCase(method)) {
            handleKeys(exchange, Integer.parseInt(keys.group(1)));
            return;
        }
        Matcher text = SESSION_TEXT.matcher(path);
        if (text.matches() && "POST".equalsIgnoreCase(method)) {
            handleText(exchange, Integer.parseInt(text.group(1)));
            return;
        }
        Matcher mouse = SESSION_MOUSE.matcher(path);
        if (mouse.matches() && "POST".equalsIgnoreCase(method)) {
            handleMouse(exchange, Integer.parseInt(mouse.group(1)));
            return;
        }

        if ("/v1/sessions".equals(path) && "GET".equalsIgnoreCase(method)) {
            handleListSessions(exchange);
            return;
        }

        sendJson(exchange, 404, error("Not found"));
    }

    private void handleListSessions(HttpExchange exchange) throws IOException {
        try {
            List<SwingDebugService.SessionSummary> sessions = service.listSessions();
            sendJson(exchange, 200, GSON.toJsonTree(sessions));
        } catch (Exception ex) {
            sendJson(exchange, 500, error(ex.getMessage()));
        }
    }

    private void handleScreen(HttpExchange exchange, int sessionId) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();
            boolean includePlanes = queryIncludes(query, "planes");
            boolean includeRaster = queryIncludes(query, "raster");
            boolean includePainted = !queryIncludes(query, "no-painted");
            SwingDebugService.ScreenCapture capture = service.captureScreen(
                    sessionId, includePlanes, includePainted, includeRaster);
            SwingRenderedScreen.Snapshot snapshot = capture.logical;
            JsonObject payload = new JsonObject();
            payload.addProperty("id", sessionId);
            payload.addProperty("rows", snapshot.rows);
            payload.addProperty("cols", snapshot.cols);
            payload.addProperty("cursorRow", snapshot.cursorRow);
            payload.addProperty("cursorCol", snapshot.cursorCol);
            payload.addProperty("cursorVisible", snapshot.cursorVisible);
            payload.addProperty("guiMode", snapshot.guiMode);
            payload.addProperty("text", snapshot.text);
            payload.addProperty("rawText", snapshot.rawText);
            if (snapshot.cursorPresentation != null) {
                JsonObject cursor = new JsonObject();
                cursor.addProperty("row", snapshot.cursorRow);
                cursor.addProperty("col", snapshot.cursorCol);
                cursor.addProperty("visible", snapshot.cursorVisible);
                cursor.addProperty("style", snapshot.cursorPresentation.style);
                cursor.addProperty("color", snapshot.cursorPresentation.color);
                cursor.addProperty("xorBase", snapshot.cursorPresentation.xorBase);
                cursor.addProperty("effectiveColor", snapshot.cursorPresentation.effectiveColor);
                payload.add("cursor", cursor);
            }
            payload.add("render", GSON.toJsonTree(snapshot.render));
            if (capture.painted != null) {
                JsonObject painted = new JsonObject();
                painted.addProperty("text", capture.painted.text);
                painted.add("foreground", GSON.toJsonTree(capture.painted.painted.foreground));
                painted.add("background", GSON.toJsonTree(capture.painted.painted.background));
                painted.addProperty("cursor", capture.painted.painted.cursor);
                painted.addProperty("differsFromRender", capture.painted.painted.differsFromRender);
                if (capture.painted.rasterBase64 != null) {
                    painted.addProperty("raster", capture.painted.rasterBase64);
                }
                payload.add("painted", painted);
            }
            if (snapshot.planes != null) {
                payload.add("planes", GSON.toJsonTree(snapshot.planes));
            }
            sendJson(exchange, 200, payload);
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 404, error(ex.getMessage()));
        } catch (Exception ex) {
            sendJson(exchange, 500, error(ex.getMessage()));
        }
    }

    private static boolean queryIncludes(String query, String token) {
        if (query == null || query.isEmpty() || token == null || token.isEmpty()) {
            return false;
        }
        for (String part : query.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int equals = part.indexOf('=');
            String key = equals >= 0 ? part.substring(0, equals) : part;
            String value = equals >= 0 ? part.substring(equals + 1) : "";
            if (!"include".equals(key)) {
                continue;
            }
            for (String item : value.split(",")) {
                if (token.equals(item.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleKeys(HttpExchange exchange, int sessionId) throws IOException {
        try {
            JsonObject body = readJsonBody(exchange);
            String keys = requiredString(body, "keys");
            service.sendKeys(sessionId, keys);
            sendJson(exchange, 200, ok());
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 404, error(ex.getMessage()));
        } catch (Exception ex) {
            sendJson(exchange, 400, error(ex.getMessage()));
        }
    }

    private void handleText(HttpExchange exchange, int sessionId) throws IOException {
        try {
            JsonObject body = readJsonBody(exchange);
            String text = requiredString(body, "text");
            service.sendText(sessionId, text);
            sendJson(exchange, 200, ok());
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 404, error(ex.getMessage()));
        } catch (Exception ex) {
            sendJson(exchange, 400, error(ex.getMessage()));
        }
    }

    private void handleMouse(HttpExchange exchange, int sessionId) throws IOException {
        try {
            JsonObject body = readJsonBody(exchange);
            int row = optionalInt(body, "row", 0);
            int col = optionalInt(body, "col", 0);
            int x = optionalInt(body, "x", -1);
            int y = optionalInt(body, "y", -1);
            int button = optionalInt(body, "button", 1);
            int clicks = optionalInt(body, "clicks", 1);
            service.sendMouse(sessionId, new SwingDebugService.MouseRequest(row, col, x, y, button, clicks));
            sendJson(exchange, 200, ok());
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 404, error(ex.getMessage()));
        } catch (Exception ex) {
            sendJson(exchange, 400, error(ex.getMessage()));
        }
    }

    private boolean authorize(HttpExchange exchange) throws IOException {
        if (authToken.isEmpty()) {
            return true;
        }
        Headers headers = exchange.getRequestHeaders();
        String authorization = headers.getFirst("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            if (authToken.equals(authorization.substring("Bearer ".length()).trim())) {
                return true;
            }
        }
        String headerToken = headers.getFirst("X-Debug-Token");
        if (authToken.equals(headerToken)) {
            return true;
        }
        sendJson(exchange, 401, error("Authentication required"));
        return false;
    }

    private static JsonObject readJsonBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            StringBuilder raw = new StringBuilder();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                raw.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
            }
            String body = raw.toString().trim();
            if (body.isEmpty()) {
                throw new IllegalArgumentException("Request body required.");
            }
            return new JsonParser().parse(body).getAsJsonObject();
        }
    }

    private static String requiredString(JsonObject body, String field) {
        if (body == null || !body.has(field) || body.get(field).isJsonNull()) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        return body.get(field).getAsString();
    }

    private static int optionalInt(JsonObject body, String field, int defaultValue) {
        if (body == null || !body.has(field) || body.get(field).isJsonNull()) {
            return defaultValue;
        }
        return body.get(field).getAsInt();
    }

    private static JsonObject ok() {
        JsonObject payload = new JsonObject();
        payload.addProperty("ok", true);
        return payload;
    }

    private static JsonObject error(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("error", message);
        return payload;
    }

    private static void sendJson(HttpExchange exchange, int status, JsonObject payload) throws IOException {
        byte[] bytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void sendJson(HttpExchange exchange, int status, com.google.gson.JsonElement payload) throws IOException {
        byte[] bytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
