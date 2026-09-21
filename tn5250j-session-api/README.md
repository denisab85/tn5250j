# tn5250j-session-api

Wire-agnostic session contracts, v1 JSON protocol DTOs, transport SPI, and RPC registry.

**Parent:** [tn5250j-parent](../README.md)  
**Depends on:** *(none from tn5250j; Gson only)*  
**Used by:** [tn5250j-session-client](../tn5250j-session-client/README.md), [tn5250j-session-server](../tn5250j-session-server/README.md)

## Purpose

Defines the **UI-facing API** and **wire message shapes** so any client (Swing, web, JavaFX) can attach to a session host without importing [tn5250j-core](../tn5250j-core/README.md) screen internals.

This module intentionally has **no TN5250, Swing, or socket code** — only interfaces, constants, Gson DTOs, and codec helpers.

## Maven coordinates

```xml
<dependency>
  <groupId>ca.denisab85</groupId>
  <artifactId>tn5250j-session-api</artifactId>
  <version>${tn5250j.version}</version>
</dependency>
```

## Core API (`org.tn5250j.session.api`)

| Type | Description |
|------|-------------|
| [`SessionClient`](src/main/java/org/tn5250j/session/api/SessionClient.java) | Lifecycle, screen access, keys, cursor, extensions |
| [`ScreenModel`](src/main/java/org/tn5250j/session/api/ScreenModel.java) | Screen geometry, planes, input, listeners |
| [`OiaModel`](src/main/java/org/tn5250j/session/api/OiaModel.java) | OIA state (insert mode, inhibited, message wait) |
| [`ConnectionProfile`](src/main/java/org/tn5250j/session/api/ConnectionProfile.java) | In-process vs remote connection descriptor |
| [`TerminalOps`](src/main/java/org/tn5250j/session/api/TerminalOps.java) | Extensible RPC: system request, debug toggle |
| [`SessionUiHooks`](src/main/java/org/tn5250j/session/api/SessionUiHooks.java) | Bell, SSL cert, SysReq, save prompts |

Host-side SPI (`org.tn5250j.session.api.host`):

| Type | Description |
|------|-------------|
| `SessionHostService` | Open/close/list/attach sessions |
| `SessionOpenRequest` | Session config DTO for `SessionOpen` |
| `SessionHandle` | Attached session control |
| `SessionEventSink` | Outbound event callback |

Transport SPI (`org.tn5250j.session.api.transport`):

| Type | Description |
|------|-------------|
| `SessionTransport` | Pluggable send/receive |
| `SessionEventSink` | Client-side event delivery |
| `TransportEndpoint` | Connection target descriptor |

## Wire protocol v1 (`org.tn5250j.session.wire`)

Single logical protocol for localhost and network. Transport: **WebSocket** with **JSON** envelopes ([`WsEnvelope`](src/main/java/org/tn5250j/session/wire/WsEnvelope.java)).

```json
{
  "v": 1,
  "type": "SendKeys",
  "id": "uuid",
  "correlationId": "uuid",
  "sessionId": "abc",
  "payload": { }
}
```

### Client → server commands

| Type constant | Purpose |
|---------------|---------|
| `HostHello` / `HostHelloAck` | Version, auth, capabilities |
| `SessionOpen` | Session config properties |
| `SessionClose` | Tear down session |
| `SessionAttach` | Subscribe / reconnect |
| `Connect` / `Disconnect` | IBM i lifecycle |
| `SendKeys` / `MoveCursor` / `SetCursor` / `SendAid` | Terminal input |
| `GetSnapshot` | Full screen + OIA resync |

### Server → client events

| Type constant | Purpose |
|---------------|---------|
| `ScreenRegionUpdated` | Dirty rectangle + plane data |
| `ScreenSizeChanged` | Row/column change |
| `OiaChanged` | OIA DTO ([`OiaStateDto`](src/main/java/org/tn5250j/session/wire/OiaStateDto.java)) |
| `SessionStateChanged` | Connected / disconnected / removed |
| `Bell` | Audible bell |
| `UiPromptRequest` / `UiPromptResponse` | Correlated SSL / SysReq / save prompts |

### RPC

```json
{ "type": "Rpc", "method": "terminal.systemRequest", "args": { "code": "90" } }
```

Registered via [`RpcRegistry`](src/main/java/org/tn5250j/session/rpc/RpcRegistry.java) on the server; invoked through `SessionClient.extension(TerminalOps.class)` on the client. See [tn5250j-session-server](../tn5250j-session-server/README.md).

Encoding helpers: [`WsMessageCodec`](src/main/java/org/tn5250j/session/wire/WsMessageCodec.java), [`ScreenPlaneCodec`](src/main/java/org/tn5250j/session/wire/ScreenPlaneCodec.java), [`ScreenSnapshotDto`](src/main/java/org/tn5250j/session/wire/ScreenSnapshotDto.java).

## Implementations

| Implementation | Module |
|----------------|--------|
| Local (wraps `Screen5250`) | [tn5250j-session-client](../tn5250j-session-client/README.md) → `LocalSessionClient` |
| Remote (WebSocket + frame buffer) | [tn5250j-session-client](../tn5250j-session-client/README.md) → `RemoteSessionClient` |
| Server bridge | [tn5250j-session-server](../tn5250j-session-server/README.md) → `SessionBridge` |

## Tests

```bash
mvn -pl tn5250j-session-api test
```

[`WsMessageCodecTest`](src/test/java/org/tn5250j/session/wire/WsMessageCodecTest.java) — golden JSON round-trip for envelope encoding.

## Security notes

| Mode | Binding | Encryption | Auth |
|------|---------|------------|------|
| In-process | N/A | N/A | N/A |
| Local WS | `127.0.0.1` | Optional | `--token` bearer |
| Network gateway | `0.0.0.0` | **TLS required** (`wss://`) | Bearer token / mTLS (future) |

IBM i credentials travel in `SessionOpen` over an encrypted channel in gateway mode; the server owns the TN5250 connection.
