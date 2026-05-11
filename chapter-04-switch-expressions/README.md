# Chapter 4: Switch Expressions

Runnable examples demonstrating the Java 14+ switch expression: arrow form,
multi-label cases, blocks with `yield`, and exhaustive switches without `default`.

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

This chapter's project uses `exec:java` (not `exec:exec`). Switch expressions
finalized in Java 14 (JEP 361) and are not preview features, so there is no
`--enable-preview` flag and the entry point is a traditional
`public static void main`.

## What you'll see

Output demonstrates:
- The legacy colon-and-`break` switch statement.
- The arrow-form switch expression returning a value.
- A multi-label case (`case MONDAY, TUESDAY -> ...`).
- A switch expression with a block body that uses `yield`.
- An exhaustive switch over an enum with no `default` clause.
