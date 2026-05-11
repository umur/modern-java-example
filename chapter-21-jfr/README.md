# Chapter 21: JFR: Built-in Observability

Companion code for Chapter 21. The program does three things:

1. Starts a 10-second JFR recording from the command line via `-XX:StartFlightRecording`.
2. Subscribes to live `jdk.GarbageCollection` events through a `RecordingStream` and prints them as they fire.
3. Defines and emits a custom `OrderProcessedEvent` for a small simulated workload.

When the JVM exits, JFR writes `demo.jfr` to the working directory. Open it in JDK Mission Control or inspect it with the `jfr` CLI.

## Status: stable, no preview flag required

JFR has been open-source since Java 11 (JEP 328). The streaming API arrived in Java 14 (JEP 349). Both are part of the standard runtime in every supported JDK build. No `--enable-preview`, no `--add-modules`.

## Prerequisites

- **JDK 25** (any vendor: Temurin, Zulu, Corretto, Liberica, Oracle, Homebrew's `openjdk@25`).
- **Maven 3.8+**.
- Optionally: **JDK Mission Control** for interactive analysis of the resulting `.jfr` file. Download from the Adoptium or Oracle download page.

If `java -version` shows something older than 25, point `JAVA_HOME` at a newer install. On macOS with Homebrew:

```bash
brew install openjdk@25
export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
```

## Run it

```bash
mvn -q clean compile exec:exec
```

That runs the demo for ten seconds. Expected output (intermixed with the live GC stream):

```
[JFR] Recording started: demo.jfr (settings=profile, duration=10s)
[Stream] Subscribed to jdk.GarbageCollection events
Processing 5000 simulated orders...
[GC] cause=G1 Evacuation Pause, duration=PT0.0042S
[GC] cause=G1 Evacuation Pause, duration=PT0.0038S
Order processed: id=order-1234, totalCents=...
...
Done. JFR file written to demo.jfr; analyze with `jfr print demo.jfr` or open in JDK Mission Control.
```

## Inspect the recording

The `jfr` CLI ships with the JDK:

```bash
# A summary view: counts by event type
jfr summary demo.jfr

# Print only custom OrderProcessed events
jfr print --events com.umur.modernjava.OrderProcessed demo.jfr

# Print every garbage collection
jfr print --events jdk.GarbageCollection demo.jfr
```

For interactive analysis, install JDK Mission Control and open `demo.jfr` from the File menu. The Automated Analysis tab is the fastest first pass.

## What's in the example

`JfrExamples` is a single program that exercises three JFR APIs:

- **`-XX:StartFlightRecording` flag** (set in the pom). The JVM starts a 10-second recording at boot, writes it to `demo.jfr` on exit. Zero application code involved.
- **`RecordingStream` consumer.** A separate stream subscribed to `jdk.GarbageCollection` events. Prints each GC pause as the JVM completes it. Demonstrates the JEP 349 live-event API.
- **Custom `OrderProcessedEvent`.** Extends `jdk.jfr.Event`, annotated with `@Name`, `@Label`, `@Category`. Emitted from a small simulated order-processing loop. Shows up in the same recording alongside the built-in events.

The simulated workload (`processOrders`) allocates and discards enough objects to make the GC do real work, so the streaming consumer has events to print and the recording has interesting GC and allocation data.

## Notes

- The recording's `settings=profile` choice is for demo purposes; it surfaces more event types than `default`. For real production always-on recording, use `settings=default` and the `maxage`/`dumponexit` options. See the chapter for the production-recommended pattern.
- The custom event is enabled because `settings=profile` enables user categories. With `settings=default`, custom events would need explicit enabling via a `.jfc` settings file or programmatic `recording.enable(...)`.
