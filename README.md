# Modern Java in Practice

> Java 21 and 25 LTS for working engineers: the features that change how you write code, not the features that look good in slides.

![Java](https://img.shields.io/badge/Java-21_LTS-ED8B00?logo=openjdk&logoColor=white) ![Java](https://img.shields.io/badge/Java-25_LTS-ED8B00?logo=openjdk&logoColor=white) ![License: MIT](https://img.shields.io/badge/License%3A_MIT-MIT-blue)

Companion code for the book **Modern Java in Practice: Java 21 and 25 LTS for Working Engineers** by [Umur Inan](https://umurinan.com).

## About the book

A focused tour of the Java language features and APIs that landed in Java 17 → 21 → 25 LTS and that an engineer should actually reach for in production code. Each chapter takes one feature. Records, sealed types, pattern matching, virtual threads, structured concurrency, scoped values, stable values, sequenced collections, gatherers, FFM, vector API, the class-file API, ZGC, JFR, leyden-aot. And shows what it changes, where it pays off, and where it doesn't.

## Prerequisites

- Java 21 LTS for most chapters ([Temurin](https://adoptium.net))
- Java 25 LTS for preview-feature chapters
- Maven 3.9+

## Quick start

```bash
git clone https://github.com/umur/modern-java-example
cd modern-java-example/chapter-02-var
mvn test
```

Each chapter is independent. No chapter depends on another.

## Layout

One Maven project per chapter:

- `chapter-01-why-modern-java`
- `chapter-02-var`: local-variable type inference
- `chapter-03-text-blocks`
- `chapter-04-switch-expressions`
- `chapter-05-records`
- `chapter-06-sealed-types`
- `chapter-07-pattern-matching`
- `chapter-08-unnamed-variables`
- `chapter-09-instance-main`
- `chapter-10-virtual-threads`
- `chapter-11-structured-concurrency`
- `chapter-12-scoped-values`
- `chapter-13-stable-values`
- `chapter-14-sequenced-collections`
- `chapter-15-gatherers`
- `chapter-16-http-client`
- `chapter-17-ffm`: Foreign Function & Memory API
- `chapter-18-vector-api`
- `chapter-19-class-file-api`
- `chapter-20-gc`: Garbage collectors in modern Java
- `chapter-21-jfr`: Java Flight Recorder
- `chapter-22-startup`: startup performance, AOT, leyden

## Stack

- Java 21 LTS (minimum); Java 25 LTS for chapters that need preview features
- Maven 3.9+
- JUnit 5

## About the author

I'm Umur Inan. I write books about Spring Boot, Java, distributed systems, and the practices that make production reliable.

📚 **More writing and books → [umurinan.com](https://umurinan.com)**

## License

MIT. See [LICENSE](LICENSE).
