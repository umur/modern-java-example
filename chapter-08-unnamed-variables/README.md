# Chapter 8: Unnamed Variables and Patterns

Runnable examples demonstrating the `_` wildcard from JEP 456: unnamed variables in locals, resources, exception handlers, and lambdas, plus unnamed patterns in record-pattern switches.

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

This chapter's project uses `exec:java` (not `exec:exec`) because there are no preview features. Unnamed variables and patterns were finalized as JEP 456 in Java 22; Java 25 ships the feature stable, so no `--enable-preview` flag is required.

## What you'll see

Output demonstrates:

- A local variable from a side-effecting call: `var _ = cache.tryAcquire(...)`.
- A `try`-with-resources block where the resource is only opened for its side effect: `try (var _ = openTransaction()) { ... }`.
- An exception handler that doesn't read the exception: `catch (NumberFormatException _)`.
- A lambda whose second parameter is unused: `events.forEach((event, _) -> log(event))`.
- A for-each loop that counts elements without reading them: `for (var _ : items) count++`.
- A switch over a sealed `OrderEvent` with record patterns whose arms ignore components they don't read: `case OrderShipped(var orderId, _, _) -> ...`.
- Nested record patterns mixing destructured, named, and unnamed components: `case Order(_, Customer(var name, _)) -> ...`.
