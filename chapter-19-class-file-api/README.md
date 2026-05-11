# Chapter 19: The Class-File API

Demonstrates the Class-File API (`java.lang.classfile`) on JDK 25. The API was finalised in Java 24 by JEP 484, after two preview rounds (JEP 457 in Java 22, JEP 466 in Java 23). As of Java 25 it is part of the standard `java.base` module: no `--enable-preview`, no `--add-modules`, no flags at all.

## Status: final, in `java.base`, no flags

| JEP | Java | Year | Status |
|-----|------|------|--------|
| 457 | 22 | 2024 | first preview |
| 466 | 23 | 2024 | second preview |
| 484 | 24 | 2025 | final |
| (stable) | 25 | 2026 | stable, no API changes |

## Prerequisites

- **JDK 24 or later** (this repo targets 25). Temurin, Zulu, Corretto, Liberica, Oracle, or Homebrew's `openjdk@25` all work.
- **Maven 3.8+**.

If `java -version` shows something older than 24, point `JAVA_HOME` at a newer install. On macOS with Homebrew:

```bash
brew install openjdk@25
export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
```

## Run

```bash
mvn -q clean compile exec:java
```

No flags are needed at compile time or run time.

## What's in the example

`ClassFileApiExamples` walks through three demos. Each prints `[demo N]` markers and observable output (method names, descriptors, byte counts, computed results), so the behaviour is visible from stdout.

1. **Reading.** Parses a `.class` file from the running JDK (`java.lang.String`'s class file, read out of the platform module via `Class.getResourceAsStream`). Prints the parsed class's name, superclass, interface count, and the first ten declared methods with their descriptors. Demonstrates that the API can introspect arbitrary class files at runtime without loading them.

2. **Writing.** Builds a brand-new class `com.umur.modernjava.ch19.Sum` from scratch, with a single `public static int sum(int, int)` method whose body is `iload_0; iload_1; iadd; ireturn`. Hands the bytes to `MethodHandles.lookup().defineClass(...)`, then reflectively invokes `Sum.sum(17, 25)` and prints the result. Demonstrates the parse-build-load-run lifecycle. The lookup's package and the generated class's package must match, so both live in `com.umur.modernjava.ch19`.

3. **Transforming.** Generates a small base class with one method, then composes a `ClassTransform` that copies every existing element through unchanged and an end-handler transform that appends a new `public static String greet()` method. Loads the transformed class, calls both the original and the new method, and prints the results. Demonstrates the immutable-model + transform pattern that replaces ASM's nested visitor chain.

The `Sum` and `Greeter` classes generated at runtime have no `.java` source. They exist only as bytes that the API produces and the JVM verifies and runs.
