# Chapter 16: The Modern HTTP Client

Demonstrates `java.net.http.HttpClient` (JEP 321, final since Java 11), running on Java 25.

## Status: final since Java 11, no flag required

JEP 321 promoted the JDK HTTP client to standard in Java 11 (September 2018), after two incubator rounds (JEP 110 in Java 9 and Java 10). The package `java.net.http` is part of the standard JDK module `java.net.http`.

| JEP | Java | Status |
|-----|------|--------|
| 110 | 9  | incubator |
| 110 | 10 | incubator |
| **321** | **11** | **final** |

No `--enable-preview` flag is needed. Any Java 11 or later JDK runs the example. The pom in this project targets release 25 to match the rest of the book.

## Prerequisites

- **JDK 11 or later** (the code in this repo is built and tested on JDK 25). Temurin, Zulu, Corretto, Liberica, Oracle, or Homebrew's `openjdk@25` all work.
- **Maven 3.8+**.

If `java -version` shows something older than 11, point `JAVA_HOME` at a newer install. On macOS:

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
mvn -q clean compile exec:java
```

## What's in the example

`HttpClientExamples` runs five demos against an in-process `com.sun.net.httpserver.HttpServer` started on a random localhost port. No network access is required; the server and client both live inside the same JVM.

1. **Sync GET.** `client.send` with `BodyHandlers.ofString()` against `/hello`. Prints status, content type, and body.
2. **Async GET.** `client.sendAsync` returning a `CompletableFuture<HttpResponse<String>>`, joined with `.join()` to keep the demo linear.
3. **POST with text body and headers.** `BodyPublishers.ofString` for the request payload, custom `Content-Type` and `X-Trace-Id` headers, response read as `String`.
4. **Streaming response as `InputStream`.** `BodyHandlers.ofInputStream()` over a 4 KB binary endpoint, copied into a buffer to demonstrate that the body is a real stream (not pre-buffered as a `String`).
5. **Parallel fanout on virtual threads.** Five concurrent sync GETs from inside a `try-with-resources` virtual-thread executor. Each call blocks on its own virtual thread; the wall time is the slowest single response, not the sum.

Each demo prints clear `request`, `response`, and `body` markers so the client behaviour is visible from stdout.
