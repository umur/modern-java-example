# Chapter 3: Text Blocks

Runnable examples demonstrating text blocks, the indentation algorithm, the new escape sequences, and `formatted()` for value splicing.

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

This chapter's project uses `exec:java` because text blocks were finalized in Java 15 (no preview features) and the entry point is a traditional `public static void main`.

## What you'll see

Output demonstrates:

- A multi-line SQL query as a text block.
- A JSON request body as a text block, parameterized with `formatted()`.
- An HTML email snippet as a text block.
- The indentation algorithm: how the position of the closing `"""` controls how much leading whitespace is stripped.
- The `\` line-continuation escape collapsing two source lines into one runtime line.
- The `\s` trailing-space escape preserving a space the stripping rule would otherwise eat.
