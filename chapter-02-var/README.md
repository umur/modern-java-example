# Chapter 2: var and Local Variable Type Inference

Runnable examples demonstrating idiomatic and unidiomatic uses of `var`.

## Prerequisites

- **JDK 25** (Temurin, Zulu, Corretto, Liberica, or Oracle).
- **Maven 3.8+**.

If `java -version` shows older than 25, set `JAVA_HOME` to a Java 25 install. On macOS:

```bash
brew install --cask temurin@25
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
```

## Run

```bash
mvn -q compile exec:java
```

Note: this chapter's project uses `exec:java` (not `exec:exec` like Chapter 1) because there are no preview features. The entry point is a traditional `public static void main`.

## What you'll see

Output demonstrates:
- A generic-soup type cleaned up with `var`.
- A factory method whose return type is inferred.
- A `for (var entry : map.entrySet())` loop.
- The "diamond trap" showing `var x = new ArrayList<>();` infers `ArrayList<Object>`.
