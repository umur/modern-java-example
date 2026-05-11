# Chapter 22: Startup and Distribution

Companion code for Chapter 22. The Java program is deliberately small (a few dozen lines, prints a couple of messages, touches `java.net.http` and `java.logging` so the runtime image story has something to work on). The interesting bits are the build and distribution steps walked through below.

## Status: stable, no preview flag required

Everything in this README except Project Leyden's AOT cache is finalised JDK functionality:

- **CDS** (Class Data Sharing): default in every JDK since 12.
- **AppCDS**: JEP 310 (Java 10), JEP 350 dynamic flow (Java 13). Stable.
- **`jlink`**: JEP 282, finalised in Java 9. Stable.
- **`jpackage`**: JEP 343 (incubator, Java 14), JEP 392 (final, Java 16). Stable.
- **AOT cache (Project Leyden)**: JEP 483, **preview** in Java 24/25. Flag names will change before final.

## Prerequisites

- **JDK 25** (any vendor: Temurin, Zulu, Corretto, Liberica, Oracle, Homebrew's `openjdk@25`).
- **Maven 3.8+**.

If `java -version` shows something older than 25, point `JAVA_HOME` at a newer install. On macOS with Homebrew:

```bash
brew install openjdk@25
export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

## Run it (the basic case)

```bash
mvn -q clean compile exec:exec
```

Expected output:

```
INFO: Hello, modern Java

=== Chapter 22: Startup and Distribution ===
JVM:               OpenJDK 64-Bit Server VM 25.0.2
JVM uptime at main: 41 ms
main() body took:   184,673 us
HttpClient class:   jdk.internal.net.http.HttpClientFacade
(Demo URI prepared but not fetched: https://example.test/never-fetched)

Hello, modern Java
```

The "JVM uptime at main" number is what we'll be trying to push down across the rest of this README.

## Build a JAR

Most of the AppCDS, jlink, and jpackage commands work against an actual JAR file rather than Maven's `target/classes` directory, so build one first:

```bash
mvn -q package -DskipTests
ls target/hello-app.jar
```

The `pom.xml` writes `Main-Class: com.umur.modernjava.ch22.HelloApp` into the manifest, so the jar is also self-launching:

```bash
java -jar target/hello-app.jar
```

## AppCDS: training and runtime

The two-step dynamic AppCDS flow (JEP 350):

```bash
# Training run: archive every class loaded during this run, on exit.
java -XX:ArchiveClassesAtExit=target/app.jsa \
     -cp target/hello-app.jar \
     com.umur.modernjava.ch22.HelloApp

# Production run: memory-map the archive on startup.
java -XX:SharedArchiveFile=target/app.jsa \
     -cp target/hello-app.jar \
     com.umur.modernjava.ch22.HelloApp
```

On a measurement run on this project's `HelloApp`, the JVM uptime at `main` drops by roughly half (from ~40ms to ~20ms on the host that produced these numbers). The absolute savings are small because `HelloApp` is small. For a Spring Boot service with thousands of classes, the savings scale up: a half-second to a second of cold-start, depending on classpath size.

A few warnings to expect:

- The training run logs `[cds] Skipping jdk/internal/event/...: JFR event class`. That's normal; JFR event classes have special init semantics that AppCDS skips. Not an error.
- The archive file is around 6-7 MB for `HelloApp` and grows with the size of the classpath being archived.
- The archive is bound to the exact JDK build that produced it. Bumping the JDK invalidates the archive; rebuild it as part of your CI pipeline.

To verify the archive is being used:

```bash
java -Xshare:on -XX:SharedArchiveFile=target/app.jsa -version
```

The third line says `mixed mode, sharing`, confirming the archive loaded. If the JVM can't load the archive (mismatched JDK, classpath drift), `-Xshare:on` makes the JVM error out instead of silently falling back, which is what you want in a CI verification step.

## Discover the modules with jdeps

`HelloApp` uses `java.base`, `java.logging`, `java.management`, and `java.net.http`. You don't have to figure that out by hand:

```bash
jdeps --print-module-deps --ignore-missing-deps target/hello-app.jar
# → java.base,java.logging,java.management,java.net.http
```

Pipe that into `jlink`.

## jlink: build a custom runtime

```bash
JDK_MODULES=$(jdeps --print-module-deps --ignore-missing-deps target/hello-app.jar)

jlink \
  --module-path "$JAVA_HOME/jmods" \
  --add-modules "$JDK_MODULES" \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --output target/runtime
```

That produces a `target/runtime/` directory weighing in around 45 MB on this project's modules (compared to the full JDK's ~300 MB). Use it as if it were a normal JDK install:

```bash
target/runtime/bin/java -cp target/hello-app.jar com.umur.modernjava.ch22.HelloApp
```

The `target/runtime/bin/java -version` confirms it's a stripped-down image:

```
openjdk version "25.0.2"
OpenJDK Runtime Environment (build 25.0.2)
OpenJDK 64-Bit Server VM (build 25.0.2, mixed mode)
```

For a Docker container, that 45 MB runtime is much smaller to ship than the full JDK and significantly smaller than the official JRE base images.

### Combining jlink with AppCDS

The runtime image and the AppCDS archive compose:

```bash
target/runtime/bin/java \
  -XX:SharedArchiveFile=target/app.jsa \
  -cp target/hello-app.jar \
  com.umur.modernjava.ch22.HelloApp
```

Both speedups apply, and the resulting Docker image is small *and* fast to start.

## jpackage: native installer

`jpackage` is host-OS-specific. The exact invocation produces different artefacts on macOS, Windows, and Linux. The command below is the common shape; pick the right `--type` for your host.

```bash
# Build the staging directory jpackage expects.
mkdir -p target/jpackage-input
cp target/hello-app.jar target/jpackage-input/

# macOS: produces target/HelloApp-1.0.0.dmg
jpackage \
  --name HelloApp \
  --app-version 1.0.0 \
  --vendor "Umur Inan" \
  --description "Modern Java in Practice, Chapter 22 demo" \
  --input target/jpackage-input \
  --main-jar hello-app.jar \
  --main-class com.umur.modernjava.ch22.HelloApp \
  --runtime-image target/runtime \
  --java-options "-XX:SharedArchiveFile=app.jsa" \
  --type dmg \
  --dest target

# Windows: --type msi (or --type exe)
# Linux:   --type deb (Debian/Ubuntu) or --type rpm (Fedora/RHEL)
```

The output is a real installer. On macOS, `target/HelloApp-1.0.0.dmg` mounts a folder you drag into Applications; the `.app` bundle inside contains the jlinked runtime, your jar, and a launcher.

This README does **not** invoke `jpackage` automatically because the output depends on the host OS and would require a CI matrix to demonstrate fully. Run the command above on whichever host you want to package for. CI for desktop applications typically fans out across at least three runners (macOS, Windows, Linux); each runs `jpackage` with the matching `--type`.

### Code signing (production)

For shipping to actual users, the installer needs to be signed:

- **macOS**: `codesign` + Apple notarization. `jpackage` does not sign for you, but it produces an `.app` bundle that `codesign` accepts.
- **Windows**: `signtool` against a code-signing certificate.
- **Linux**: GPG-sign the `.deb` or `.rpm` if your distribution channel requires it.

None of those are `jpackage` problems; they're platform-distribution problems. The Apple and Microsoft documentation are the authoritative sources.

## Project Leyden AOT cache (preview)

JEP 483, *Ahead-of-Time Class Loading & Linking*, is in **preview** as of Java 25. The flag names have changed once already and may change again before the JEP finalises. Treat this section as a sketch, not a stable recipe.

The three-step flow:

```bash
# (1) Training run: record what classes get loaded and linked.
java -XX:AOTMode=record \
     -XX:AOTConfiguration=target/app.aotconf \
     -cp target/hello-app.jar \
     com.umur.modernjava.ch22.HelloApp

# (2) Build the cache from the recording.
java -XX:AOTMode=create \
     -XX:AOTConfiguration=target/app.aotconf \
     -XX:AOTCache=target/app.aot \
     -cp target/hello-app.jar

# (3) Production run: use the cache.
java -XX:AOTCache=target/app.aot \
     -cp target/hello-app.jar \
     com.umur.modernjava.ch22.HelloApp
```

If the flag names above don't work against your JDK, check `java -XX:+UnlockDiagnosticVMOptions -XX:+PrintFlagsFinal | grep -i aot` and the JEP page (`openjdk.org/jeps/483`) for the current spelling.

The AOT cache addresses one layer above what AppCDS does: it skips not just classloading but also the linking step (resolving symbolic references in the constant pool, building the resolved method tables, etc.). On top of an AppCDS-warmed run, the additional savings are typically in the same order: another hundred milliseconds for a non-trivial application. As Leyden lands more JEPs (compiled-code caching, profile caching), the cumulative savings grow.

## What this code project does *not* do

- **Run jpackage automatically.** Output depends on the host OS; see above.
- **Run a Leyden AOT training run automatically.** Flag names are unstable.
- **Compile to GraalVM Native Image.** Out of scope for this chapter; see the GraalVM project documentation if you want to try it. The `pom.xml` is a normal JVM project, not a Native Image one.

## What's in the source

- `HelloApp.java`: a single class. Touches `java.net.http` (HttpClient creation, no actual request), `java.logging` (one log line), `java.lang.management` (RuntimeMXBean to report JVM uptime). Prints a few status lines and exits.

## Going further

- **For Maven users**: `maven-jlink-plugin` integrates jlink into the Maven lifecycle for fully modular projects. For classpath projects, the `exec-maven-plugin` invocation in this README is fine.
- **For Gradle users**: `org.beryx.jlink` is the de-facto standard plugin for jlink + jpackage from Gradle.
- **For Spring Boot**: the Spring Boot Maven plugin's `process-aot` goal is unrelated to the Project Leyden AOT cache; it's Spring's own bean-graph-precomputation mechanism for Native Image. Don't confuse the two.
