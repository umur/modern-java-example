# Chapter 18: The Vector API

Demonstrates the Vector API (`jdk.incubator.vector`) on JDK 25. The API has been incubating since Java 16 (JEP 338) and is still incubating in Java 25. Its finalisation is gated on Project Valhalla (value classes). Until Valhalla lands, the API ships behind an `--add-modules jdk.incubator.vector` flag and method names may shift between JDK releases.

## Status: incubating, requires `--add-modules jdk.incubator.vector`

| JEP | Java | Year | Status |
|-----|------|------|--------|
| 338 | 16 | 2021 | first incubator |
| 414 | 17 | 2021 | second |
| 417 | 18 | 2022 | third |
| 426 | 19 | 2022 | fourth |
| 438 | 20 | 2023 | fifth |
| 448 | 21 | 2023 | sixth |
| 460 | 22 | 2024 | seventh |
| 469 | 23 | 2024 | eighth |
| 489 | 24 | 2025 | ninth |
| 25 incubator | 25 | 2026 | tenth, still incubating |

The code in this repo targets JDK 25.

## Prerequisites

- **JDK 16 or later** (this repo targets 25). Temurin, Zulu, Corretto, Liberica, Oracle, or Homebrew's `openjdk@25` all work.
- **Maven 3.8+**.

If `java -version` shows something older than 16, point `JAVA_HOME` at a newer install. On macOS with Homebrew:

```bash
brew install openjdk@25
export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
```

## A note on `--add-modules`

Incubator modules require an explicit `--add-modules jdk.incubator.vector` flag at *both* compile time and run time. This is different from preview features, which use `--enable-preview`. The pom passes `--add-modules` to the compiler plugin and to `exec:exec`, so `mvn -q clean compile exec:exec` works without any extra setup.

If you run the compiled class directly with `java`, add the flag yourself:

```bash
java --add-modules jdk.incubator.vector -cp target/classes com.umur.modernjava.ch18.VectorApiExamples
```

## Run

```bash
mvn -q clean compile exec:exec
```

## What's in the example

`VectorApiExamples` walks through three demos. Each prints clear `[demo N]` markers and the observable values, so the behaviour is visible from stdout.

1. **Species and lane count.** Prints what `IntVector.SPECIES_PREFERRED` reports for the host CPU. On AVX2 you'll see 8 lanes; on AVX-512, 16; on Apple Silicon NEON, 4.

2. **Sum of squares: scalar vs vector.** Two implementations of the same numeric kernel over a million-element `int[]`. The scalar version is the obvious `for` loop; the vector version uses the `IntVector` species, `loopBound`, lane multiplication, and `reduceLanes(VectorOperators.ADD)`. Both run multiple warmup iterations before the timed pass to give the JIT a fair chance, then print the timings and the speedup ratio.

3. **Dot product: scalar vs vector.** Same shape as demo 2 but with two arrays. The recommendation-engine kernel from the chapter overview.

The exact speedup depends on the CPU and the JIT version. On a 2024 Mac with NEON the dot product is roughly 3x; on AVX2 servers, closer to 4x; on AVX-512 hardware, 6-8x. If your numbers are smaller, the JIT may already have been auto-vectorising the scalar version, which is the most common reason a Vector API rewrite buys nothing.
