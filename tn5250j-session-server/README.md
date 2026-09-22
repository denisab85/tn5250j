# tn5250j-session-server

Session host service and WebSocket gateway that bridges [tn5250j-core](../tn5250j-core/README.md) sessions to remote UIs.

**Parent:** [tn5250j-parent](../README.md)  
**Depends on:** [tn5250j-session-api](../tn5250j-session-api/README.md), [tn5250j-core](../tn5250j-core/README.md)  
**Used by:** [tn5250j-swing](../tn5250j-swing/README.md) (embedded `--server` mode), [tn5250j](../tn5250j/README.md) (shaded JAR)

## Purpose

Runs **`Session5250`** instances and exposes them over the v1 JSON/WebSocket protocol defined in [tn5250j-session-api](../tn5250j-session-api/README.md). Supports:

- **Local daemon** — UI/display split on the same PC (`--bind 127.0.0.1`)
- **Network gateway** — core near IBM i, UI elsewhere (`--bind 0.0.0.0`, use `wss://`)

Remote clients connect via [tn5250j-session-client](../tn5250j-session-client/README.md) (`RemoteSessionClient`).

## Maven coordinates

```xml
<dependency>
  <groupId>ca.denisab85</groupId>
  <artifactId>tn5250j-session-server</artifactId>
  <version>${tn5250j.version}</version>
</dependency>
```

Standalone JAR manifest entry point: `org.tn5250j.session.server.SessionServerMain`

## Running

### From shaded product JAR

```bash
java -jar tn5250j/target/tn5250j-0-SNAPSHOT.jar --server --port 5250 --bind 127.0.0.1
```

### Standalone entry point

The module JAR is thin and requires its runtime dependencies. The shaded product
JAR supplies them; build from the repository root, then invoke the server class:

```bash
mvn -Pshaded package
java -cp tn5250j/target/tn5250j-0-SNAPSHOT.jar \
  org.tn5250j.session.server.SessionServerMain \
  --port 5250 --bind 127.0.0.1 --token my-secret
```

Use `--help`/`-h` for generated usage. Short options are `-b` (bind),
`-P` (port), and `-k` (token); standalone mode also accepts `-p` for port.

### Option migration

| Former | New POSIX form |
|--------|----------------|
| `-server` | `--server` / `-S` (desktop launcher) |
| *(listener)* | `--bind` / `-b`, `--port` / `-P`, `--token` / `-k` |

Full desktop and session mapping: [CLI reference](../docs/CLI.md#option-migration).

| Option | Default | Description |
|--------|---------|-------------|
| `--bind` | `127.0.0.1` | Listen address |
| `--port` | `5250` | WebSocket port |
| `--token` | *(empty)* | Optional bearer token checked on `HostHello` |

Output: `tn5250j session server listening on ws://HOST:PORT`

## Components

| Class | Role |
|-------|------|
| [`SessionServerMain`](src/main/java/org/tn5250j/session/server/SessionServerMain.java) | CLI entry point |
| [`WebSocketSessionServer`](src/main/java/org/tn5250j/session/server/WebSocketSessionServer.java) | WebSocket listener, message dispatch, RPC |
| [`DefaultSessionHostService`](src/main/java/org/tn5250j/session/server/DefaultSessionHostService.java) | Implements `SessionHostService` — open/close/list/attach |
| [`SessionBridge`](src/main/java/org/tn5250j/session/server/SessionBridge.java) | Per-session adapter: core listeners → wire events |
| [`RemoteUiHooks`](src/main/java/org/tn5250j/session/server/RemoteUiHooks.java) | Blocks VT thread on `UiPromptRequest` until client responds |
| [`ScreenSnapshotBuilder`](src/main/java/org/tn5250j/session/server/wire/ScreenSnapshotBuilder.java) | Builds `ScreenSnapshotDto` for `GetSnapshot` |
| [`ScreenPlaneEncoder`](src/main/java/org/tn5250j/session/server/wire/ScreenPlaneEncoder.java) | Encodes `Screen5250` planes for wire push |

### Event flow

```
Screen5250 (ScreenListener)
    → SessionBridge
    → ScreenRegionUpdated (WebSocket)
    → RemoteSessionClient
    → RemoteScreenModel frame buffer
    → Swing GuiGraphicBuffer
```

Core event types reused: [`ScreenListener`](../tn5250j-core/src/main/java/org/tn5250j/event/ScreenListener.java), [`SessionListener`](../tn5250j-core/src/main/java/org/tn5250j/event/SessionListener.java), [`ScreenOIAListener`](../tn5250j-core/src/main/java/org/tn5250j/event/ScreenOIAListener.java).

### RPC methods (v1)

Registered in `WebSocketSessionServer` via [`RpcRegistry`](../tn5250j-session-api/src/main/java/org/tn5250j/session/rpc/RpcRegistry.java):

| Method | Purpose |
|--------|---------|
| `terminal.systemRequest` | Host system request (SysReq) |
| `terminal.toggleDebug` | VT debug flag |

Future: `spool.export`, `transfer.start` (Phase 3).

## Tests

```bash
mvn -pl tn5250j-session-server test
```

[`WebSocketSessionIntegrationTest`](src/test/java/org/tn5250j/session/server/WebSocketSessionIntegrationTest.java) — embedded server + `RemoteSessionClient` connect/attach cycle (uses `Tn5250TestHarness` from core test-jar).

## Security

For non-loopback deployment:

- Bind with TLS (`wss://`) — configure at reverse proxy or extend `WebSocketSessionServer`
- Require `--token` or mTLS
- Never log IBM i credentials from `SessionOpen` payloads

See security table in [tn5250j-session-api/README.md](../tn5250j-session-api/README.md).

## Related

- Client: [tn5250j-session-client](../tn5250j-session-client/README.md)
- Swing remote flag: [tn5250j-swing](../tn5250j-swing/README.md)

Command-line options and local setup: [CLI reference](../docs/CLI.md).
