# Command-line reference

Both the desktop launcher (`org.tn5250j.My5250`) and standalone session server
(`org.tn5250j.session.server.SessionServerMain`) use [picocli](https://picocli.info/).
Use `--help` or `-h` to see their options without opening Swing or a network listener.

## Build and launch

From the repository root, with Java 8+ and Maven 3.6+:

```bash
mvn -Pshaded package
```

The default version is `0-SNAPSHOT`; substitute your version if built with
`-Drevision=...`. Use the product JAR, not a sources, javadoc, or `original-` JAR.
The `shaded` profile includes runtime dependencies, including picocli. Plain
`mvn package` produces a thin product JAR that needs third-party dependencies on
the classpath. Use `-Pshaded-full` if you need optional scripting/PDF features.

```bash
# Desktop with in-process sessions; choose a saved session in the dialog
java -jar tn5250j/target/tn5250j-0-SNAPSHOT.jar

# Connect directly to an IBM i host
java -jar tn5250j/target/tn5250j-0-SNAPSHOT.jar \
  --enhanced --code-page 37 ibmi.example.net

# Open named saved sessions (quote names containing spaces)
java -jar tn5250j/target/tn5250j-0-SNAPSHOT.jar \
  --session "Production" -s "Test system"
```

For separate core and Swing processes on the same machine, leave terminal 1 running:

```bash
# Terminal 1: core hosted by the session server
java -jar tn5250j/target/tn5250j-0-SNAPSHOT.jar \
  --server --bind 127.0.0.1 --port 5250 --token local-secret

# Terminal 2: Swing, connecting to the server; choose the IBM i session in the dialog
java -jar tn5250j/target/tn5250j-0-SNAPSHOT.jar \
  --new-instance --remote ws://127.0.0.1:5250 --remote-token local-secret

# Alternatively, specify the IBM i host directly
java -jar tn5250j/target/tn5250j-0-SNAPSHOT.jar \
  -n -r ws://127.0.0.1:5250 --remote-token local-secret \
  --host-port 992 --ssl-type TLS ibmi.example.net
```

Omit both token options if authentication is not needed on localhost. The server
owns the IBM i connection. The WebSocket listener port and the IBM i TN5250 port
are separate: `--port`/`-P` configures the listener; `--host-port`/`-p` configures
IBM i. Stop the server with Ctrl+C. Use `wss://` with a TLS-terminating proxy for
connections outside the local machine.

To invoke the standalone server entry point with all dependencies available:

```bash
java -cp tn5250j/target/tn5250j-0-SNAPSHOT.jar \
  org.tn5250j.session.server.SessionServerMain \
  --bind 127.0.0.1 --port 5250 --token local-secret
```

The individual `tn5250j-session-server` module JAR is thin; its manifest alone
does not supply its runtime dependencies. In an IDE, use that module's runtime
classpath for `SessionServerMain`, or the Swing module's runtime classpath for
`My5250`. Use the same program arguments as above (omit `--server` when invoking
`SessionServerMain` directly).

## Option migration

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

## Desktop options

Long names use two dashes and hyphens between words. Short names use a single
dash and one letter. Short names are case-sensitive: `-s` and `-S` differ.

| Long option | Short | Purpose |
|---|---|---|
| `--help` | `-h` | Print usage and exit |
| `--server` | `-S` | Run the WebSocket session server without Swing |
| `--remote URL` | `-r` | Use a `ws://` or `wss://` server |
| `--remote-token TOKEN` | `-T` | Token for `--remote` |
| `--session NAME` | `-s` | Open a saved session; repeat to open several |
| `--new-instance` | `-n` | Bypass forwarding to an existing desktop |
| `--daemon` | `-d` | Accept desktop launch requests on localhost:3036 |
| `--locale LOCALE` | `-L` | Locale such as `en_US` or `en-US` |
| `--width PIXELS` | `-W` | Positive initial window width |
| `--height PIXELS` | `-H` | Positive initial window height |
| `--host-port PORT` | `-p` | IBM i TN5250 port |
| `--config FILE` | `-f` | Local session properties resource |
| `--code-page CCSID` | `-c` | Host code page |
| `--enhanced` | `-e` | Enhanced TN5250 negotiation |
| `--gui` | `-g` | Graphical terminal enhancements |
| `--name-from-system` | `-t` | Use the configured session name as title |
| `--wide` | `-w` | 27x132 screen; otherwise 24x80 |
| `--proxy` | `-u` | Enable SOCKS proxy |
| `--proxy-host HOST` | | SOCKS proxy host |
| `--proxy-port PORT` | | SOCKS proxy port |
| `--ssl-type TYPE` | | TLS/SSL implementation, e.g. `TLS` |
| `--device-name NAME` | `-N` | Requested terminal device name |
| `--device-name-from-hostname` | | Use this machine's hostname as device name |
| `--heartbeat` | `-B` | Enable heartbeat |
| `--new-window` | `-o` | Separate window per session |

The optional positional `HOST` is the IBM i hostname or IP address. It may occur
before or after options. Choose either `HOST` or `--session`; using both is an
error. Without either, normal default/last-session and connection-dialog behavior
applies. Explicit session options override corresponding saved properties; flags
enable features without clearing other saved settings. Remote URL and token
options apply to saved, restored, and dialog-selected sessions as well as direct
host launches. A new remote URL does not inherit a token saved for another URL.
`--config` is used for local sessions; remote sessions use the transmitted session
properties, not a client-side properties file.

`--daemon` is the historical desktop single-instance forwarding service, not the
headless session server. `--new-instance` bypasses that service and takes
precedence over `--daemon`. Forwarding requires a desktop running this version
to understand the new option names and quoted values.

## Server options

These are accepted by the desktop launcher **with `--server`**, or directly by
`SessionServerMain` without `--server`.

| Long option | Short | Default | Purpose |
|---|---|---|---|
| `--bind ADDRESS` | `-b` | `127.0.0.1` | Listen address |
| `--port PORT` | `-P` | `5250` | WebSocket listen port |
| `--token TOKEN` | `-k` | Empty | Require the same client token |
| `--help` | `-h` | | Print usage and exit |

The standalone server also accepts `-p` for its listen port. In the desktop
launcher `-p` always means IBM i port, and is rejected with `--server`; use `-P`
or `--port` instead. Desktop/session options cannot be combined with `--server`.

## Debug interface options

Enable a localhost HTTP API on the **Swing desktop** for screen snapshots and input
(for humans and automation agents). Full API reference: [DEBUG-INTERFACE.md](DEBUG-INTERFACE.md).

| Long option | Short | Default | Purpose |
|---|---|---|---|
| `--debug-interface` | `-A` | off | Enable the debug HTTP API |
| `--debug-bind ADDRESS` | | `127.0.0.1` | Debug API listen address |
| `--debug-port PORT` | | `5036` | Debug API listen port |
| `--debug-token TOKEN` | | Empty | Require token on debug API requests |

Cannot be combined with `--server`. Example:

```bash
java -jar tn5250j/target/tn5250j-0-SNAPSHOT.jar \
  --debug-interface --debug-token agent-secret --session "Production"
```

## Syntax

Options follow the usual GNU/POSIX conventions: long options use two dashes
(`--host-port`), short options use one dash and one letter (`-p`), and short
flags may be clustered (`-neg` is `-n -e -g`). Values may be separate
(`--code-page 37`), attached to long options (`--code-page=37`), or attached to
short options (`-c37`, `-p992`).
- Use `--` to end option processing, e.g. `-- -unusual-hostname`.
- Quote values containing spaces using your shell's quoting rules.
- `@file` expansion is disabled: filenames and tokens beginning with `@` stay literal.
- Unknown options, missing values, extra hosts, invalid integers/ports, and
  conflicting modes report usage errors instead of being silently ignored
  or failing later. Ports must be in `1..65535`. Repeated scalar options are
  normally errors; `--session` is repeatable.
- `--help` succeeds without starting Swing or binding a socket. Exit code `0`
  means success/help, `2` means invalid command-line usage, and `1` means a
  synchronous launch failure. Asynchronous connection failures are reported by
  the existing session/network code.
- Saved session definitions use the same option names. Editing/saving a session
  writes long names, with quoted values where needed.
