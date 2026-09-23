# tn5250j-swing

Desktop Swing UI for TN5250J: session panels, terminal rendering, dialogs, and the main application entry point.

**Parent:** [tn5250j-parent](../README.md)  
**Depends on:** [tn5250j-core](../tn5250j-core/README.md), [tn5250j-session-client](../tn5250j-session-client/README.md), [tn5250j-session-server](../tn5250j-session-server/README.md)  
**Assembled by:** [tn5250j](../tn5250j/README.md) product module

## Purpose

Provides the **graphical terminal emulator** users interact with. After the session-layer refactor, the UI talks to [`SessionClient`](../tn5250j-session-api/README.md) instead of holding direct references to [`Session5250`](../tn5250j-core/README.md) — enabling the same UI code for in-process and remote sessions.

## Maven coordinates

```xml
<dependency>
  <groupId>ca.denisab85</groupId>
  <artifactId>tn5250j-swing</artifactId>
  <version>${tn5250j.version}</version>
</dependency>
```

Optional dependencies (macros, PDF, L&F) are marked `optional`; see [product README](../tn5250j/README.md).

## Entry point

Main class: **`org.tn5250j.My5250`** (also set in parent POM `mainClass`)

```bash
java -cp tn5250j-swing-*.jar:tn5250j-core-*.jar:... org.tn5250j.My5250
# Prefer the shaded JAR from tn5250j module instead
```

CLI parsing is implemented in [`org.tn5250j.cli`](src/main/java/org/tn5250j/cli/) (`DesktopOptions`, `SessionOptions`).

### Option migration

Historical single-dash multi-letter spellings (such as `-cp`, `-server`, or
`-132`) are no longer accepted. Use the POSIX forms below:

| Former | New POSIX form |
|--------|----------------|
| `-server` | `--server` / `-S` |
| `-remote` | `--remote` / `-r` |
| `-remoteToken` | `--remote-token` / `-T` |
| `-nc` | `--new-instance` / `-n` |
| `-width` | `--width` / `-W` |
| `-height` | `--height` / `-H` |
| `-cp` | `--code-page` / `-c` |
| `-gui` | `--gui` / `-g` |
| `-132` | `--wide` / `-w` |
| `-usp` | `--proxy` / `-u` |
| `-sph` | `--proxy-host` |
| `-spp` | `--proxy-port` |
| `-sslType` | `--ssl-type` |
| `-dn` | `--device-name` / `-N` |
| `-dn=hostname` | `--device-name-from-hostname` |
| `-hb` | `--heartbeat` / `-B` |
| `-noembed` | `--new-window` / `-o` |
| `-e` | `--enhanced` / `-e` |
| `-p` | `--host-port` / `-p` |
| `-f` | `--config` / `-f` |
| `-t` | `--name-from-system` / `-t` |
| `-s` | `--session` / `-s` |
| `-d` | `--daemon` / `-d` |
| `-L` | `--locale` / `-L` |
| `host` (applet param) | `HOST` (positional argument) |

Full reference: [CLI reference](../docs/CLI.md).

## Session integration

### SessionPanel

[`SessionPanel`](src/main/java/org/tn5250j/SessionPanel.java) is the central view:

```java
// Preferred constructor
SessionPanel panel = new SessionPanel(sessionClient);

// Legacy — delegates to SessionClientFactory.wrapLocal(session)
SessionPanel panel = new SessionPanel(session5250);
```

Key types used internally:

| Was (direct core) | Now |
|-------------------|-----|
| `Screen5250 screen` | `ScreenModel screen` via `client.getScreen()` |
| `session.getVT().systemRequest(...)` | `client.extension(TerminalOps.class)` |
| `session.getVT()` for spool/transfer | Guarded — `null` on remote sessions |

[`GuiGraphicBuffer`](src/main/java/org/tn5250j/GuiGraphicBuffer.java) renders from `ScreenModel` planes and listens via `ScreenListener` / `OiaModelListener`.

[`SwingApiUiHooks`](src/main/java/org/tn5250j/gui/SwingApiUiHooks.java) bridges Swing dialogs to API [`SessionUiHooks`](../tn5250j-session-api/src/main/java/org/tn5250j/session/api/SessionUiHooks.java).

### Connection modes (My5250)

| Flag | Effect |
|------|--------|
| *(none)* | In-process session via `SessionClientFactory.wrapLocal()` |
| `--server [--bind] [--port] [--token]` | Run [session server](../tn5250j-session-server/README.md) without opening Swing |
| `--remote URL [--remote-token TOKEN]` | Open `SessionPanel` backed by `RemoteSessionClient` |
| `--debug-interface [-A] [--debug-bind] [--debug-port] [--debug-token]` | Localhost HTTP API for screen state and input |

Example — split UI and core on one machine:

```bash
# Terminal 1
java -jar tn5250j-0-SNAPSHOT.jar --server --port 5250

# Terminal 2
java -jar tn5250j-0-SNAPSHOT.jar --remote ws://127.0.0.1:5250
```

## Debug interface (agents)

With `--debug-interface`, the desktop exposes a JSON HTTP API on localhost for:

- Reading the rendered screen as plain text (plus cursor position)
- Sending key mnemonics, literal text, and mouse clicks

Default port: **5036**. See [DEBUG-INTERFACE.md](../docs/DEBUG-INTERFACE.md) for the full API and agent workflow.

## Key packages

| Package | Role |
|---------|------|
| `org.tn5250j` | `My5250`, `SessionPanel`, `GuiGraphicBuffer`, main frame |
| `org.tn5250j.debug` | Swing debug HTTP API (`SwingDebugServer`, `SwingRenderedScreen`) |
| `org.tn5250j.gui` | Dialogs, UI hooks, settings |
| `org.tn5250j.keyboard` | Key maps, actions, `KeyboardHandler` |
| `org.tn5250j.spoolfile` | Spool export (requires local `tnvt`) |
| `org.tn5250j.tools` | Macros, file transfer, utilities |
| `org.tn5250j.mailtools` | Email screen capture |

## Features requiring local session

These still call `Session5250.getVT()` or `Screen5250` directly and are disabled or limited on remote sessions:

- Spool file export ([`SpoolExporter`](src/main/java/org/tn5250j/spoolfile/SpoolExporter.java))
- File transfer ([`TransferAction`](src/main/java/org/tn5250j/keyboard/actions/TransferAction.java))
- Host print via VT ([`SessionPopup`](src/main/java/org/tn5250j/SessionPopup.java))
- [`PrinterThread`](src/main/java/org/tn5250j/PrinterThread.java) screen printing

Terminal operations available remotely via **`TerminalOps`** RPC ([`DispMsgsAction`](src/main/java/org/tn5250j/keyboard/actions/DispMsgsAction.java)).

## EDT dispatching

VT events are marshalled to the Swing EDT via [`SwingEdtDispatcher`](src/main/java/org/tn5250j/SwingEdtDispatcher.java) (installed on local `Session5250`). Remote clients receive WebSocket events on a background thread and should dispatch UI updates on the EDT the same way.

## Tests

```bash
mvn -pl tn5250j-swing test
```

Notable tests:

- `SessionPanelInputTest` — keyboard, mouse wheel, keypad
- `SessionEventFanoutTest` — `GuiGraphicBuffer` registered on `ScreenModel` / `OiaModel`
- `My5250Test` — CLI argument parsing

Uses `tn5250j-core` test-jar (`Tn5250TestHarness`).

## Related

- Session API contracts: [tn5250j-session-api](../tn5250j-session-api/README.md)
- Client implementations: [tn5250j-session-client](../tn5250j-session-client/README.md)
- Runnable product JAR: [tn5250j](../tn5250j/README.md)

Command-line options and local setup: [CLI reference](../docs/CLI.md).
