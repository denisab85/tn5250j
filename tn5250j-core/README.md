# tn5250j-core

GUI-free 5250 session engine: TN5250 transport, screen model, encoding, and session lifecycle.

**Parent:** [tn5250j-parent](../README.md)  
**Consumed by:** [tn5250j-session-client](../tn5250j-session-client/README.md), [tn5250j-session-server](../tn5250j-session-server/README.md), [tn5250j-swing](../tn5250j-swing/README.md)

## Purpose

This module is the IBM i terminal emulator **without any UI**. It handles:

- TN5250 socket I/O and SSL ([`tnvt`](src/main/java/org/tn5250j/framework/tn5250/tnvt.java))
- 5250 screen parsing and field model ([`Screen5250`](src/main/java/org/tn5250j/framework/tn5250/Screen5250.java))
- EBCDIC / CCSID code pages ([`encoding`](src/main/java/org/tn5250j/encoding))
- Session configuration and manager ([`Session5250`](src/main/java/org/tn5250j/Session5250.java), [`SessionManager`](src/main/java/org/tn5250j/framework/common/SessionManager.java))

Remote session layers ([session-api](../tn5250j-session-api/README.md)) wrap this module rather than replacing it.

## Maven coordinates

```xml
<dependency>
  <groupId>ca.denisab85</groupId>
  <artifactId>tn5250j-core</artifactId>
  <version>${tn5250j.version}</version>
</dependency>
```

Test utilities (e.g. `Tn5250TestHarness`) are available as a test-jar:

```xml
<dependency>
  <groupId>ca.denisab85</groupId>
  <artifactId>tn5250j-core</artifactId>
  <version>${tn5250j.version}</version>
  <type>test-jar</type>
  <scope>test</scope>
</dependency>
```

## Key packages

| Package | Role |
|---------|------|
| `org.tn5250j` | `Session5250`, `SessionConfig`, constants |
| `org.tn5250j.framework.tn5250` | Screen, VT, data stream, OIA |
| `org.tn5250j.framework.transport` | Socket connector, SSL |
| `org.tn5250j.framework.common` | `SessionManager`, session registry |
| `org.tn5250j.encoding` | Code page factories and CCSID converters |
| `org.tn5250j.event` | `ScreenListener`, `SessionListener`, OIA events |
| `org.tn5250j.interfaces` | `SessionController`, `SessionView`, `SessionUiHooks` (local boundaries) |
| `org.tn5250j.keyboard` | Key mnemonics (`KeyMnemonic`) shared with UI |

## UI boundaries (local only)

Before the session-api layer, UIs coupled directly to core types:

- **`Session5250`** — connect/disconnect, VT access, session events
- **`Screen5250`** — screen planes, cursor, keys, field model
- **`SessionUiHooks`** — SSL prompts, SysReq, save-settings dialogs

The Swing module now prefers [`SessionClient`](../tn5250j-session-api/README.md) but still uses core types for in-process-only features (spool export, file transfer, printing). See [tn5250j-swing](../tn5250j-swing/README.md).

## Dependencies

- `log4j` — logging
- `jt400` — IBM i toolbox (encoding helpers)

No Swing, Gson, or WebSocket libraries.

## Build

```bash
mvn -pl tn5250j-core test
mvn -pl tn5250j-core package   # produces main + sources + javadoc + test-jar
```

Artifact: `tn5250j-core/target/tn5250j-core-*.jar`
