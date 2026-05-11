# Chapter 1: Hello, Modern Java

A minimal example demonstrating Java 25's instance main method and `IO.println` (both preview as of Java 25).

## Prerequisites

- **JDK 25** (Temurin, Zulu, Corretto, Liberica, or Oracle).
- **Maven 3.8+**.

If `java -version` shows something older than 25, point `JAVA_HOME` at a Java 25 install. On macOS:

```bash
brew install --cask temurin@25
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
```

Or use `sdk install java 25-tem` with sdkman.

## Run

```bash
mvn -q compile exec:exec
```

Expected output:

```
Hello, modern Java.
This file uses two Java 25 features: instance main methods and IO.println.
Both are preview as of Java 25. The rest of the book uses finalized features unless noted.
```

## Why preview?

Instance main methods (JEP 512 in Java 25, fourth preview) and the new `java.lang.IO` class are not finalized yet. They're worth previewing because they change how every Java file you'll ever write opens.
