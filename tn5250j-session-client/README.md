# tn5250j-session-client

Local and remote implementations of [`SessionClient`](../tn5250j-session-api/README.md).

**Parent:** [tn5250j-parent](../README.md)  
**Depends on:** [tn5250j-session-api](../tn5250j-session-api/README.md), [tn5250j-core](../tn5250j-core/README.md)  
**Used by:** [tn5250j-swing](../tn5250j-swing/README.md)

## Purpose

Provides the client side of the session architecture:

- **In-process fast path** — zero serialization; wraps existing [`Session5250`](../tn5250j-core/README.md)
- **Remote path** — WebSocket transport, client-side frame buffer, RPC stubs

UIs obtain a client through [`SessionClientFactory`](src/main/java/org/tn5250j/session/client/SessionClientFactory.java); they should not construct `LocalSessionClient` / `RemoteSessionClient` directly unless testing.

## Maven coordinates

```xml
<dependency>
  <groupId>ca.denisab85</groupId>
  <artifactId>tn5250j-session-client</artifactId>
  <version>${tn5250j.version}</version>
</dependency>
```

Transitive: `tn5250j-session-api`, `tn5250j-core`, `Java-WebSocket`.

## Factory usage

### In-process (default desktop path)

```java
Session5250 session = SessionManager.instance().openSession(props, configResource, name);
SessionClient client = SessionClientFactory.wrapLocal(session);
client.connect();
ScreenModel screen = client.getScreen();
```

### Remote

```java
ConnectionProfile profile = ConnectionProfile.remote(
    "ws://127.0.0.1:5250", "token", "my-session", hostProperties);
SessionClient client = SessionClientFactory.create(profile);
client.setUiHooks(new MyUiHooks());
client.connect();
```

Swing wiring: [My5250](../tn5250j-swing/src/main/java/org/tn5250j/My5250.java) (`-remote` flag) and [SessionPanel](../tn5250j-swing/src/main/java/org/tn5250j/SessionPanel.java).

## Package layout

| Package / class | Role |
|-----------------|------|
| `SessionClientFactory` | Creates local or remote clients from `ConnectionProfile` |
| `InProcessTransport` | Same-JVM bridge to server-side `SessionBridge` (no socket) |
| `local.LocalSessionClient` | Wraps `Session5250`; delegates to `LocalScreenModel` |
| `local.LocalScreenModel` | Adapts `Screen5250` → `ScreenModel`; fans out screen events |
| `local.LocalTerminalOps` | In-process `TerminalOps` via `tnvt` |
| `local.UiHooksBridge` | Maps API `SessionUiHooks` → core `SessionUiHooks` |
| `remote.RemoteSessionClient` | WebSocket lifecycle, attach, snapshot resync |
| `remote.RemoteScreenModel` | Client frame buffer updated from push events |
| `remote.ScreenFrameBuffer` | Text/attr/field plane storage |
| `remote.WebSocketTransport` | JSON envelope send/receive |
| `remote.RemoteTerminalOps` | RPC for `terminal.systemRequest` |

## Local vs remote behavior

| Feature | Local | Remote |
|---------|-------|--------|
| Screen updates | Direct `Screen5250` listeners | `ScreenRegionUpdated` → frame buffer |
| `unwrap(Session5250.class)` | Returns session | Returns `null` |
| `getVT()` (via Swing) | Available | `null` — use `TerminalOps` RPC |
| Spool / transfer / print | Full | Guarded or unavailable |
| `copyTextField` | Full field model | Best-effort / stub |

## Reconnection

Remote clients send `SessionAttach` + `GetSnapshot` to resync the frame buffer after a WebSocket drop. See protocol in [tn5250j-session-api](../tn5250j-session-api/README.md).

## Tests

```bash
mvn -pl tn5250j-session-client test
```

[`LocalSessionClientTest`](src/test/java/org/tn5250j/session/client/local/LocalSessionClientTest.java) — verifies `ScreenModel` and `TerminalOps` exposure through the local adapter.

## Related

- Server counterpart: [tn5250j-session-server](../tn5250j-session-server/README.md)
- Protocol DTOs: [tn5250j-session-api](../tn5250j-session-api/README.md)
