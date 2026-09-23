# Swing debug interface

The Swing desktop can expose a **localhost HTTP API** that lets humans and automation agents read the rendered terminal screen and send keyboard or mouse input — without scraping pixels or attaching a debugger.

This is separate from the WebSocket **session server** (`--server`). The debug interface controls the **Swing UI** itself: what the user sees and how input reaches the session panel.

## Quick start

Build the shaded JAR, then launch the desktop with the debug API enabled:

```bash
mvn -Pshaded package

java -jar tn5250j/target/tn5250j-0-SNAPSHOT.jar \
  --debug-interface \
  --debug-bind 127.0.0.1 \
  --debug-port 5036 \
  --debug-token agent-secret \
  --session "Production"
```

Verify the API is up:

```bash
curl -H "Authorization: Bearer agent-secret" http://127.0.0.1:5036/health
# {"ok":true}
```

Read the active screen (session `0`):

```bash
curl -H "Authorization: Bearer agent-secret" http://127.0.0.1:5036/v1/sessions/0/screen
```

## When to use this

| Goal | Use |
|------|-----|
| Drive the **Swing window** the user sees | `--debug-interface` (this API) |
| Split core and UI across processes | `--server` + `--remote` ([session server](../tn5250j-session-server/README.md)) |
| Default single-JVM desktop | No flags needed |

The debug interface works with **in-process** and **remote** Swing sessions. It always reads and inputs through the Swing `SessionPanel`, not the raw VT thread.

## Security

- Binds to **`127.0.0.1` by default**. Do not expose this port on a network interface.
- Set **`--debug-token`** in any shared or long-lived environment. Without a token, any local process can control open sessions.
- Auth headers (when a token is configured):
  - `Authorization: Bearer <token>`
  - `X-Debug-Token: <token>`
- Cannot be combined with `--server` (headless session server mode).

## Command-line options

| Long option | Short | Default | Purpose |
|---|---|---|---|
| `--debug-interface` | `-A` | off | Enable the HTTP debug API |
| `--debug-bind ADDRESS` | | `127.0.0.1` | Listen address |
| `--debug-port PORT` | | `5036` | Listen port |
| `--debug-token TOKEN` | | empty | Require token on every request |

See also: [CLI reference](CLI.md).

## HTTP API reference

Base URL: `http://127.0.0.1:5036` (or your chosen bind/port).

All responses are JSON. Request bodies for `POST` endpoints must be `Content-Type: application/json`.

### `GET /`

Service discovery. Returns endpoint paths.

### `GET /health`

Liveness check.

**Response `200`:**

```json
{"ok": true}
```

### `GET /v1/sessions`

List open Swing session tabs, in display order.

**Response `200`:**

```json
[
  {"id": 0, "name": "Production", "connected": true},
  {"id": 1, "name": "Test", "connected": false}
]
```

| Field | Meaning |
|-------|---------|
| `id` | Session index used by other endpoints |
| `name` | Session name / tab label |
| `connected` | Whether the session is connected to IBM i |

### `GET /v1/sessions/{id}/screen`

Returns the current screen as **plain text**, using the same visibility rules as the Swing renderer (`GuiGraphicBuffer`):

- Non-display and attribute positions appear as spaces
- Duplicate characters (`0x1C`) appear as `*`
- GUI border cells are mapped to ASCII (`+`, `-`, `|`)
- Other non-printable characters appear as `.`

**Response `200`:**

```json
{
  "id": 0,
  "rows": 24,
  "cols": 80,
  "cursorRow": 12,
  "cursorCol": 5,
  "cursorVisible": true,
  "guiMode": false,
  "text": "Sign On\nSystem  . . . . . . :   CDKDEV\n..."
}
```

| Field | Meaning |
|-------|---------|
| `rows`, `cols` | Screen dimensions |
| `cursorRow`, `cursorCol` | Cursor position (**1-based**, IBM i convention) |
| `cursorVisible` | Whether the cursor is currently shown |
| `guiMode` | Whether GUI enhancements are active |
| `text` | Full screen, newline-separated rows |

### `POST /v1/sessions/{id}/input/keys`

Send **key mnemonics** through the same path as the on-screen keypad.

**Request body:**

```json
{"keys": "[enter]"}
```

Examples: `[enter]`, `[tab]`, `[pf3]`, `[pf12]`, `[help]`, `[roll up]`.

**Response `200`:** `{"ok": true}`

### `POST /v1/sessions/{id}/input/text`

Type **literal text** into the current field (uses paste/field input, not individual key events).

**Request body:**

```json
{"text": "USER01"}
```

**Response `200`:** `{"ok": true}`

### `POST /v1/sessions/{id}/input/mouse`

Simulate a mouse click on the session panel. Uses the same listeners as a real user click (moves cursor when applicable).

**Request body (preferred — screen coordinates):**

```json
{"row": 12, "col": 5, "clicks": 1, "button": 1}
```

**Request body (alternative — view pixel coordinates):**

```json
{"x": 120, "y": 180, "clicks": 1, "button": 1}
```

| Field | Default | Meaning |
|-------|---------|---------|
| `row`, `col` | — | **1-based** screen cell (either this pair or `x`/`y` required) |
| `x`, `y` | — | Pixel coordinates within the session panel |
| `clicks` | `1` | Click count (`2` triggers double-click handling when enabled) |
| `button` | `1` | `1` = left, `3` = right |

**Response `200`:** `{"ok": true}`

### Error responses

| Status | When |
|--------|------|
| `401` | Missing or wrong token |
| `404` | Unknown session id |
| `400` | Invalid JSON or missing required fields |
| `405` | Wrong HTTP method |
| `500` | Internal error on the Swing EDT |

Error body:

```json
{"error": "Unknown session id: 99"}
```

## Agent workflow

Recommended loop for automation agents:

1. **`GET /v1/sessions`** — pick the target `id` (usually `0` for the first tab).
2. **`GET /v1/sessions/{id}/screen`** — parse `text`, `cursorRow`, `cursorCol`, and `connected`.
3. Decide the next action from screen content (sign-on, menu, error message, etc.).
4. **`POST .../input/text`** or **`POST .../input/keys`** to enter data or press keys.
5. **`POST .../input/mouse`** when a specific field must be focused by position.
6. Repeat from step 2 until the task is done.

### Sign-on example

```bash
BASE=http://127.0.0.1:5036
AUTH="Authorization: Bearer agent-secret"
SID=0

curl -H "$AUTH" "$BASE/v1/sessions/$SID/screen"

curl -X POST -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"text":"USER01"}' "$BASE/v1/sessions/$SID/input/text"
curl -X POST -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"keys":"[tab]"}' "$BASE/v1/sessions/$SID/input/keys"
curl -X POST -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"text":"SECRETPWD"}' "$BASE/v1/sessions/$SID/input/text"
curl -X POST -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"keys":"[enter]"}' "$BASE/v1/sessions/$SID/input/keys"

curl -H "$AUTH" "$BASE/v1/sessions/$SID/screen"
```

### Agent notes

- **Session ids are ephemeral.** They are list indices, not stable across restarts. Always call `GET /v1/sessions` first.
- **Screen text is a snapshot.** Poll `/screen` after each input action; there is no push/stream API in v1.
- **Coordinates are 1-based** for `row`/`col`, matching IBM i screen positions.
- **`/input/keys` vs `/input/text`:** use `keys` for function keys and AIDs; use `text` for literal field content.
- **Remote sessions:** when Swing connects via `--remote`, the debug API still reflects the Swing-rendered screen (which mirrors the remote session state).
- **Threading:** all operations are marshalled onto the Swing EDT; rapid-fire requests are serialized.

## Implementation

| Component | Location |
|-----------|----------|
| HTTP server | `org.tn5250j.debug.SwingDebugServer` |
| EDT service | `org.tn5250j.debug.SwingDebugService` |
| Screen capture | `org.tn5250j.debug.SwingRenderedScreen` |
| CLI options | `org.tn5250j.debug.DebugInterfaceOptions` |
| Startup | `My5250.launch()` when `--debug-interface` is set |

## Related

- [CLI reference](CLI.md) — all desktop flags
- [Architecture](ARCHITECTURE.md) — session server vs in-process modes
- [tn5250j-swing README](../tn5250j-swing/README.md) — Swing module overview
