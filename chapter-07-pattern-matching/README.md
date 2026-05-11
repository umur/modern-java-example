# Chapter 7: Pattern Matching

Runnable examples demonstrating pattern matching: `instanceof` patterns from Java 16, switch patterns and record patterns from Java 21, guarded cases, and a Visitor-to-sealed-switch refactor.

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

This chapter's project uses `exec:java` (not `exec:exec`) because there are no preview features. Pattern matching for `instanceof` has been final since Java 16; switch patterns and record patterns since Java 21.

## What you'll see

Output demonstrates:

- An `instanceof` pattern with a binding, used to skip the explicit cast.
- The early-return idiom: `if (!(event instanceof T t)) return;` then use `t` for the rest of the method.
- A switch expression over a sealed `OrderEvent` hierarchy with type patterns and no `default`.
- A record-pattern switch destructuring each `OrderEvent` permit's components inline.
- Nested record patterns walking a `Customer`/`Address`/`Order` tree.
- A guarded case: `case Discount d when d.percent() > 50 -> "premium"`.
- A Visitor-style expression evaluator refactored into a sealed switch.
