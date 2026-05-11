# Chapter 12: Scoped Values

Demonstrates `ScopedValue` (JEP 506) on Java 25.

## Status: final in Java 25, no flag required for `ScopedValue` itself

`ScopedValue` finalised in Java 25 under JEP 506:

| JEP | Java | Status |
|-----|------|--------|
| 429 | 20 | incubator |
| 446 | 21 | first preview |
| 464 | 22 | second preview |
| 481 | 23 | third preview |
| 487 | 24 | fourth preview |
| **506** | **25** | **final** |

The `ScopedValue` class lives in `java.lang`. No `--enable-preview` is required to use it.

## Why this project still uses `--enable-preview`

The example combines scoped values with structured concurrency, which is *still* preview as of Java 25 (JEP 505, fifth preview). Because we use `StructuredTaskScope` to demonstrate the fork-inheritance behaviour, the project enables preview features in the pom. If you want to use scoped values without preview, the relevant demos (1, 3, 4) compile and run perfectly without the flag; only demo 2 needs it.

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

`ScopedValueExamples` runs four demos in sequence:

1. **Basic binding and read.** A `ScopedValue<String>` for the request ID is bound at the entry point and read from a method three frames deep without being passed as a parameter.
2. **Fork inheritance with structured concurrency.** A `StructuredTaskScope` forks three subtasks; each fork reads the parent's binding without explicit propagation. (This demo needs `--enable-preview` because `StructuredTaskScope` is still preview.)
3. **Reading an unbound value throws.** Calling `.get()` outside any binding throws `NoSuchElementException`. The demo catches and prints the exception to show this is the right behaviour.
4. **Rebinding inside a sub-scope.** An inner `where(KEY, newValue).run(...)` overrides the outer value for the duration of the inner block, then the outer value comes back when the inner block exits.

Each demo prints the bound (or unbound) value at each frame so the dynamic-extent semantics are visible from stdout.
