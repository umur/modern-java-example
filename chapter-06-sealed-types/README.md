# Chapter 6: Sealed Types

Runnable examples demonstrating sealed types: a sealed interface with record permits modeling a domain sum type, an exhaustive switch expression with type patterns over the sealed parent, and a sealed class with a `non-sealed` branch.

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

Note: this chapter's project uses `exec:java` (not `exec:exec`) because there are no preview features. Sealed types have been final since Java 17.

## What you'll see

Output demonstrates:
- A `sealed interface OrderEvent` with three record permits (`OrderPlaced`, `OrderShipped`, `OrderCancelled`).
- A switch expression over `OrderEvent` with type patterns and no `default`, covering every permit.
- A `sealed class Shape` with `Circle` and `Square` records and a `non-sealed class Polygon` that's further extended by a `Hexagon` subclass.
- A switch expression over `Shape` showing how the `non-sealed` branch absorbs the open subhierarchy at compile time.
