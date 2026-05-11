# Chapter 13: Stable Values

Demonstrates `StableValue<T>` (JEP 502) on Java 25.

## Preview status

Stable values are **first preview as of Java 25 (September 2025)**. JEP 502 is the first round of the API:

| JEP | Java | Status |
|-----|------|--------|
| **502** | **25** | **first preview** |

The class lives in `java.lang.StableValue` (a sealed interface). Both compile and run require `--enable-preview`. Names and signatures may shift before the API finalises; pin your JDK and isolate the imports.

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

`StableValueExamples` runs four demos in sequence:

1. **Per-instance lazy field.** A `TaxCalculator` holds a `StableValue<Map<String, BigDecimal>>`. The expensive load runs at most once per instance, the first time `rateFor` is called. Subsequent reads are cheap.
2. **Race-safe initialisation under contention.** Twenty virtual threads race to read the same fresh stable value. The supplier runs exactly once. The threads that lose the race read the winner's value.
3. **Stable list for a fixed-size lazy array.** A 256-slot `StableValue.list(...)` where each slot is computed on first read. Reading slot 42 does not initialise slots 0..41 or 43..255.
4. **Stable map for a fixed-key lazy registry.** A `StableValue.map(...)` keyed on region codes, where each region's `TaxCalculator` is constructed on first read of that key.

Each demo prints clear `init`, `read`, and `result` markers so the at-most-once guarantee and the per-slot laziness are visible from stdout.
