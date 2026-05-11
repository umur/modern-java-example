# Chapter 11: Structured Concurrency

Demonstrates `StructuredTaskScope` (JEP 505) on Java 25.

## Preview status

Structured concurrency is **still preview as of Java 25**. It has been preview since Java 19:

| JEP | Java | Status |
|-----|------|--------|
| 428 | 19 | first incubator |
| 437 | 20 | second incubator |
| 453 | 21 | first preview, API redesigned |
| 462 | 22 | second preview |
| 480 | 23 | third preview |
| 499 | 24 | fourth preview |
| 505 | 25 | fifth preview |

The API in this code project targets the JEP 505 shape: `StructuredTaskScope.open(Joiner)` plus `Joiner.awaitAllSuccessfulOrThrow()`, `Joiner.anySuccessfulResultOrThrow()`, and `Joiner.awaitAll()`. Older tutorials show `ShutdownOnFailure` and `ShutdownOnSuccess` subclasses; those compiled against Java 21 and do not compile here. The shape is stable; expect names to settle when the feature finalises.

## Prerequisites

- **JDK 25** (Temurin, Zulu, Corretto, Liberica, Oracle, or Homebrew's `openjdk@25`).
- **Maven 3.8+**.

If `java -version` shows something older than 25, point `JAVA_HOME` at a Java 25 install. On macOS:

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
mvn -q clean compile exec:exec
```

The `--enable-preview` flag is wired into both compile and exec via the pom. You don't need to pass it on the command line.

## What's in the example

`StructuredConcurrencyExamples` runs four demos in sequence:

1. **Fan-out with fail-fast (`Joiner.awaitAllSuccessfulOrThrow`).** Three simulated I/O calls in parallel. All succeed. Total wall time tracks the slowest one.
2. **Race-the-fastest (`Joiner.anySuccessfulResultOrThrow`).** Two replicas of the same call. The fastest wins; the slower one is cancelled.
3. **Open scope (`Joiner.awaitAll`).** Five tasks fan out, each prints "started", "finished" or "cancelled", and the main thread inspects the results.
4. **Failing fork cancels siblings.** One fork throws; the other two are running `Thread.sleep` and react to interrupts. The exception propagates out of `scope.join()` and the cancelled siblings print "cancelled".

Each demo prints clear `started`, `finished`, `result`, or `cancelled` markers so you can see the cancellation flow.

## Why preview?

JEP 505 is the fifth preview round. The shape is stable; the names have moved between releases (the Java 21 API used `ShutdownOnFailure`/`ShutdownOnSuccess` subclasses, the Java 25 API uses a `Joiner` factory). Pin your JDK, keep the import in one place, and expect to revise it once when the feature finalises.
