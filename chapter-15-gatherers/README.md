# Chapter 15: Stream Gatherers

Demonstrates JEP 485 (Java 24, final) stream gatherers and the new `Stream::gather` intermediate operation, on Java 25.

## Status: final since Java 24, no flag required

JEP 485 finalised in Java 24 (March 2025), after two preview rounds (JEP 461 in Java 22, JEP 473 in Java 23). The interfaces and methods are part of the standard `java.util.stream` API:

| JEP | Java | Status |
|-----|------|--------|
| 461 | 22 | preview |
| 473 | 23 | second preview |
| **485** | **24** | **final** |

No `--enable-preview` flag is needed. Any Java 24 or later JDK runs the example.

## Prerequisites

- **JDK 24 or later** (the code in this repo is built and tested on JDK 25). Temurin, Zulu, Corretto, Liberica, Oracle, or Homebrew's `openjdk@25` all work.
- **Maven 3.8+**.

If `java -version` shows something older than 24, point `JAVA_HOME` at a newer install. On macOS:

```bash
brew install --cask temurin@25
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
```

If you installed with Homebrew (`brew install openjdk@25`), the install lives outside `/Library/Java/JavaVirtualMachines`, so `/usr/libexec/java_home` won't find it. Set `JAVA_HOME` directly:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
```

Or use sdkman: `sdk install java 25-tem`.

## Run

```bash
mvn -q clean compile exec:java
```

## What's in the example

`GathererExamples` runs five demos in sequence:

1. **`Gatherers.windowFixed(3)`** over a stream of ten integers. Three full windows of three plus a trailing partial window of one.
2. **`Gatherers.windowSliding(3)`** over the same input. Eight overlapping windows of three.
3. **`Gatherers.fold(...)`** to accumulate a stream of strings into a single comma-joined value, emitted as a one-element stream.
4. **`Gatherers.scan(...)`** to compute a running prefix sum. One output per input, each carrying the cumulative total.
5. **A custom `dedupAdjacent()` gatherer.** Fifteen lines including its private state class. Collapses runs of consecutive equal elements without removing non-adjacent duplicates.

Each demo prints clear `input` and `output` markers so the gatherer behaviour is visible from stdout.
