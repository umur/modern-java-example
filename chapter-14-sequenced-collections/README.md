# Chapter 14: Sequenced Collections

Demonstrates `SequencedCollection`, `SequencedSet`, and `SequencedMap` (JEP 431) on Java 25.

## Status: final since Java 21, no flag required

JEP 431 finalised in Java 21 (September 2023). The interfaces and methods are part of the standard `java.util` API:

| JEP | Java | Status |
|-----|------|--------|
| **431** | **21** | **final** |

No `--enable-preview` flag is needed. Any Java 21 or later JDK runs the example.

## Prerequisites

- **JDK 21 or later** (the code in this repo is built and tested on JDK 25). Temurin, Zulu, Corretto, Liberica, Oracle, or Homebrew's `openjdk@25` all work.
- **Maven 3.8+**.

If `java -version` shows something older than 21, point `JAVA_HOME` at a newer install. On macOS:

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

`SequencedCollectionExamples` runs four demos in sequence:

1. **`LinkedHashMap` first/last access without the iterator dance.** Shows `firstEntry()`, `lastEntry()`, `pollFirstEntry()`, and the "ten most recent" pattern using `reversed().sequencedValues().stream().limit(10)`.
2. **`List.reversed()` returns a view, not a copy.** Mutates the original after taking a reversed view, then mutates through the reversed view, and prints both lists each step so the live-view contract is visible.
3. **`LinkedHashSet` first/last access.** Demonstrates `getFirst`, `getLast`, `addFirst`, `addLast` on a `SequencedSet`. Shows that `addFirst` of a duplicate moves the existing element to the head.
4. **A method written to take `SequencedCollection<T>`.** Same body, called with a `List`, an `ArrayDeque`, and a `LinkedHashSet`. The point: the wider parameter type accepts every ordered collection in the JDK.

Each demo prints clear `before` and `after` markers so the structural changes are visible from stdout.
