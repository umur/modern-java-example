# Chapter 9: Instance Main Methods and the Quiet On-Ramp

Code for chapter 9. Demonstrates JEP 512 (Java 25, fourth preview): instance main methods, the four accepted launcher signatures, and the implicitly declared class form.

## Preview status

JEP 512 is **preview** in Java 25. Compilation and execution both require `--enable-preview`. Class files compiled this way are pinned to JDK 25; they will not run on a different JDK without recompiling. That is the deliberate cost of using the feature before it is finalized (expected in Java 26 or 27).

## Prerequisites

- **JDK 25** (Temurin, Zulu, Corretto, Liberica, or Oracle).
- **Maven 3.8+**.

If `java -version` shows something older than 25, point `JAVA_HOME` at a Java 25 install. On macOS:

```bash
brew install --cask temurin@25
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
```

Or use `sdk install java 25-tem` with sdkman.

## Run the Maven examples

```bash
mvn -q clean compile exec:exec
```

This compiles and runs `InstanceMainExamples`, which itself demonstrates:

1. An instance `main()` (no args, no `static`).
2. A traditional `public static void main(String[] args)`.
3. An instance `main(String[] args)`.

The implicit-class file `Hello.java` is **excluded from the Maven build** because it has no `package` declaration and does not fit Maven's conventional source layout. Run it with the source launcher instead.

## Run the implicit-class example

```bash
java --enable-preview --source 25 \
     src/main/java/com/umur/modernjava/ch09/Hello.java
```

The source launcher compiles the file in memory and runs it. No build step, no `target/`, no `.class` left behind.

Expected output:

```
Hello, World.
Hello, modern Java.
```

## Files

- `pom.xml` — Maven config with `--enable-preview` for both compile and exec, and an exclude for `Hello.java`.
- `src/main/java/com/umur/modernjava/ch09/InstanceMainExamples.java` — entry point. Uses an instance `void main()`, calls helpers, delegates to the other forms.
- `src/main/java/com/umur/modernjava/ch09/EchoArgs.java` — instance `main(String[] args)` example.
- `src/main/java/com/umur/modernjava/ch09/TraditionalMain.java` — the 35-year-old static form, for contrast.
- `src/main/java/com/umur/modernjava/ch09/Hello.java` — implicit-class form. Run via the source launcher, not Maven.
