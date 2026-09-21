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
| `tn5250j/target/tn5250j-*.jar` | Default **thin shaded** JAR — all project modules, minimal third-party bundling |
| `-Pshaded` | **Lean fat JAR** — core runtime deps (log4j, jt400, gson, websocket, mail); excludes optional features |
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

Third-party libraries (log4j, jt400, Java-WebSocket, Gson, JavaMail) are **not** included in the default thin shaded JAR — add them to the classpath or use `-Pshaded`.

## Run

```bash
# Build lean runnable JAR
mvn -Pshaded package

# Desktop (in-process sessions)
java -jar tn5250j/target/tn5250j-*.jar

# Session server only
java -jar tn5250j/target/tn5250j-*.jar -server --port 5250

# Remote UI client
java -jar tn5250j/target/tn5250j-*.jar -remote ws://127.0.0.1:5250
```

See [root README](../README.md) for full run modes and [tn5250j-swing](../tn5250j-swing/README.md) for CLI flags.

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
