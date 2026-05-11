# Chapter 5: Records

Runnable examples demonstrating records: simple shapes, compact constructor validation, interface implementation, nested records, the auto-generated `equals`/`hashCode`/`toString`, and multi-return tuples.

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

Note: this chapter's project uses `exec:java` (not `exec:exec`) because there are no preview features. Records have been final since Java 16.

## What you'll see

Output demonstrates:
- A simple `Customer` record and its auto-generated accessors.
- An `OrderTotal` record whose compact constructor rejects null and negative amounts.
- A `Money` record implementing `Comparable<Money>` and adding helper methods.
- A nested `CsvImporter.Result` record returned from `importFile`.
- An equals/hashCode/toString demonstration showing structural equality.
- A multi-return tuple (`Result(int found, int errors)`) used as a method return.
