# tn5250j (product)

Shaded desktop product JAR that assembles all tn5250j modules into a runnable application.

**Parent:** [tn5250j-parent](../README.md)  
**Depends on:** [tn5250j-core](../tn5250j-core/README.md), [tn5250j-swing](../tn5250j-swing/README.md), [tn5250j-session-server](../tn5250j-session-server/README.md)  
**Main class:** `org.tn5250j.My5250`

## Purpose

This module contains **no application source code**. It is the Maven assembly point for:

- Merging project JARs (core, swing, session-api, session-client, session-server)
- Producing thin, lean shaded, full shaded, and release artifacts
- Setting the executable manifest (`Main-Class`)

End users and CI typically build and run this artifact rather than individual modules.

## Artifacts

After `mvn package`:

| Output | Description |
|--------|-------------|
| `tn5250j/target/tn5250j-0-SNAPSHOT.jar` | Default **thin shaded** JAR — all project modules, minimal third-party bundling |
| `-Pshaded` | **Lean fat JAR** — runtime deps (logback, jt400, gson, websocket, mail, picocli); excludes optional features |
| `-Pshaded-full` | **Full fat JAR** — includes Jython, iText, BouncyCastle, Kunststoff |
| `-Prelease` | Lean fat JAR + GPG sign + Maven Central publish |

### What gets shaded (default thin JAR)

```
tn5250j-core
tn5250j-swing
tn5250j-session-api
tn5250j-session-client
tn5250j-session-server
```

Third-party libraries (logback, jt400, Java-WebSocket, Gson, JavaMail, picocli) are **not** included in the default thin shaded JAR — add them to the classpath or use `-Pshaded`.

## Run

```bash
# Build lean runnable JAR
mvn -Pshaded package

# Desktop (in-process sessions)
java -jar tn5250j/target/tn5250j-0-SNAPSHOT.jar

# Session server only
java -jar tn5250j/target/tn5250j-0-SNAPSHOT.jar --server --port 5250

# Remote UI client
java -jar tn5250j/target/tn5250j-0-SNAPSHOT.jar --remote ws://127.0.0.1:5250
```

See [root README](../README.md) for full run modes and [tn5250j-swing](../tn5250j-swing/README.md) for CLI flags.

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

## Maven Central

The `-Prelease` profile publishes **`ca.denisab85:tn5250j`** (lean shaded JAR) to Maven Central.

Library consumers who embed only the engine should depend on **`tn5250j-core`** or **`tn5250j-session-api`** directly — not this product coordinate. See module table in [root README](../README.md).

## Module documentation

| Module | README |
|--------|--------|
| Core engine | [tn5250j-core/README.md](../tn5250j-core/README.md) |
| Session API | [tn5250j-session-api/README.md](../tn5250j-session-api/README.md) |
| Session client | [tn5250j-session-client/README.md](../tn5250j-session-client/README.md) |
| Session server | [tn5250j-session-server/README.md](../tn5250j-session-server/README.md) |
| Swing UI | [tn5250j-swing/README.md](../tn5250j-swing/README.md) |

Command-line options and local setup: [CLI reference](../docs/CLI.md).
