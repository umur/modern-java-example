# Chapter 17: The Foreign Function & Memory API

Demonstrates the FFM API (`java.lang.foreign`), JEP 454, final since Java 22.

## Status: final since Java 22, no preview flag required

| JEP | Java | Status |
|-----|------|--------|
| 370 | 14 | incubator (memory access only) |
| 383, 393 | 15, 16 | incubator |
| 412, 419 | 17, 18 | incubator (memory access + foreign linker) |
| 424, 434, 442 | 19, 20, 21 | preview |
| **454** | **22** | **final** |

The API has been final and unflagged since Java 22 (March 2024). The code in this repo is built and tested on JDK 25.

## Prerequisites

- **JDK 22 or later** (this repo targets 25). Temurin, Zulu, Corretto, Liberica, Oracle, or Homebrew's `openjdk@25` all work.
- **Maven 3.8+**.

If `java -version` shows something older than 22, point `JAVA_HOME` at a newer install. On macOS with Homebrew:

```bash
brew install --cask temurin@25
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
```

## A note on `--enable-native-access`

FFM's restricted methods (anything that calls into native code or reinterprets memory) print a warning on first use unless you pass `--enable-native-access=<modules>` (or `ALL-UNNAMED` for unmodularized code). The pom in this project sets that flag for the `exec:java` invocation, so the demo runs without warnings. If you run the compiled class directly with `java`, add the flag yourself:

```bash
java --enable-native-access=ALL-UNNAMED -cp target/classes com.umur.modernjava.ch17.FfmExamples
```

The warning becomes an error in a future JDK release. Add the flag now, not later.

## Run

```bash
mvn -q clean compile exec:java
```

## What's in the example

`FfmExamples` walks through five FFM patterns. No external native library is required: the only native function called is `strlen` from the C standard library, which is part of every Unix-like system and the C runtime on Windows.

1. **Off-heap allocation with a confined arena.** Allocates 10 ints off-heap, writes a small sequence into them, reads them back. Demonstrates `Arena.ofConfined()`, `MemorySegment`, and `ValueLayout.JAVA_INT`.

2. **Struct layout with a `VarHandle`.** Defines a `Point` struct (`int x`, `int y`) via `MemoryLayout.structLayout`, allocates one, writes the fields with derived `VarHandle`s, reads them back.

3. **Sequence of structs.** Allocates an array of 5 `Point` structs in one block of off-heap memory, walks the array with a sequence-element access path, prints each one.

4. **Calling `strlen` from libc.** The canonical native-call demo: looks up `strlen` via `Linker.nativeLinker().defaultLookup()`, builds a `FunctionDescriptor`, gets a `MethodHandle`, calls it with a UTF-8 C string in off-heap memory.

5. **Bounds checking in action.** Tries to read past the end of a small segment, catches the resulting `IndexOutOfBoundsException`. The point is to show the safety story: `Unsafe` would have read whatever was at that address; FFM throws.

Each demo prints clear `[demo N]` markers and the output values so the behaviour is visible from stdout.
