# TN5250J
A 5250 terminal emulator for the IBM i (AS/400) written in Java.

Documentation is available at: [tn5250j.github.io](https://tn5250j.github.io/)

[![Build Status](https://travis-ci.org/tn5250j/tn5250j.svg?branch=travis)](https://travis-ci.org/tn5250j/tn5250j)

## Build (Maven)

Requirements:
- Java 8+
- Maven

Default `mvn package` builds three modules (`tn5250j-core`, `tn5250j-swing`, and the `tn5250j` assembly). The **thin** library artifact for consumers is `tn5250j-core/target/tn5250j-core-*.jar`. Optional feature dependencies are marked optional so library consumers do not pull them transitively.

Maven Central (`-Prelease`) publishes the **lean** shaded JAR (core deps only — no Jython, iText, BouncyCastle, or Kunststoff).

### Optional feature dependencies

Add these on the classpath (or as Maven dependencies) only if you need the feature:

| Feature | Dependency |
| --- | --- |
| Macros / scripting | `org.python:jython-standalone:2.7.3` |
| Spool → PDF export | `com.lowagie:itext:2.1.7` (plus BouncyCastle `bcprov-jdk14` / `bcmail-jdk14` / `bctsp-jdk14` 1.38 if required by your iText setup) |

### Shaded desktop JARs

Lean fat JAR (usable desktop run without macros/PDF/L&F extras) — same layout as the Central release artifact:

```bash
mvn -Pshaded package
```

Full fat JAR (includes Jython, iText, BouncyCastle, and Kunststoff L&F):

```bash
mvn -Pshaded-full package
```

Run either shaded JAR:

```bash
java -jar tn5250j/target/tn5250j-*.jar
```

## History

This project was created because there was no terminal emulator for Linux with features like continued edit fields, gui windows, cursor progression fields, etc.

It was then open sourced to give something back to all those hackers and code churners that work so hard to provide the Linux and Open Source communities with quality work and software.

The original developer wanted it to work on different operating systems, thus Java giving the “J” at the end.

## Hosting

The project was previous hosted at [sourceforge.net](https://sourceforge.net/projects/tn5250j/). But since 2016 has been migrated to GitHub.
