# Chapter 20: Garbage Collectors Today

Companion code for Chapter 20. The program is an allocation-heavy demo that runs through the same workload under whichever garbage collector the JVM is configured to use, then prints the throughput and a short GC summary.

The interesting output is not the program's stdout. It is the GC log lines that appear on stderr alongside it.

## Status: all collectors final, no flags required

The collectors covered in this chapter are all production-ready as of Java 25:

| Collector | Final since | Selector flag |
|-----------|-------------|---------------|
| Serial | Java 1.0 | `-XX:+UseSerialGC` |
| Parallel | Java 1.4 | `-XX:+UseParallelGC` |
| G1 | Java 7 (default since 9) | `-XX:+UseG1GC` |
| ZGC (generational) | Java 21 (JEP 439) | `-XX:+UseZGC` |
| Shenandoah (generational) | Java 24 (JEP 404) | `-XX:+UseShenandoahGC` |
| Epsilon (no-op) | Java 11 | `-XX:+UseEpsilonGC -XX:+UnlockExperimentalVMOptions` |

No `--enable-preview`, no `--add-modules`. Pick a collector flag, set the heap, turn on the GC log.

## Prerequisites

- **JDK 25** (any vendor: Temurin, Zulu, Corretto, Liberica, Oracle, Homebrew's `openjdk@25`).
- **Maven 3.8+**.
- A platform that ships ZGC and Shenandoah in your JDK build. All major OpenJDK distributors do as of 2026.

If `java -version` shows something older than 21, point `JAVA_HOME` at a newer install. On macOS with Homebrew:

```bash
brew install openjdk@25
export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
```

## Run with the project default (G1)

```bash
mvn -q clean compile exec:exec
```

That runs the demo with `-Xmx512m -Xms512m -XX:+UseG1GC -Xlog:gc`. The application prints the active GC bean, the throughput numbers, the heap state, and a per-collector summary. Mixed in on stderr: the GC log lines from `-Xlog:gc`.

## Run with a different collector

The pom configures a default flag set; override it on the command line via `-Dexec.args` to swap collectors:

```bash
# Generational ZGC
mvn -q exec:exec -Dexec.args="-Xmx512m -Xms512m -XX:+UseZGC -Xlog:gc -classpath target/classes com.umur.modernjava.ch20.GcDemo"

# Generational Shenandoah (if your JDK build includes it)
mvn -q exec:exec -Dexec.args="-Xmx512m -Xms512m -XX:+UseShenandoahGC -Xlog:gc -classpath target/classes com.umur.modernjava.ch20.GcDemo"

# Parallel (the throughput collector)
mvn -q exec:exec -Dexec.args="-Xmx512m -Xms512m -XX:+UseParallelGC -Xlog:gc -classpath target/classes com.umur.modernjava.ch20.GcDemo"

# Serial (single-threaded, tiny-heap collector)
mvn -q exec:exec -Dexec.args="-Xmx512m -Xms512m -XX:+UseSerialGC -Xlog:gc -classpath target/classes com.umur.modernjava.ch20.GcDemo"
```

Or skip Maven and run directly off the compiled class files after the first `mvn compile`:

```bash
java -Xmx512m -Xms512m -XX:+UseZGC -Xlog:gc \
     -classpath target/classes \
     com.umur.modernjava.ch20.GcDemo
```

For more verbose GC logging, replace `-Xlog:gc` with `-Xlog:gc*` (all GC categories) or `-Xlog:gc*:file=gc.log:time,uptime:filecount=10,filesize=10M` (rotating file output, the production-ready form).

## What's in the example

`GcDemo` does three things:

1. **Reports the active GC.** Reads `GarbageCollectorMXBean` to print which GC beans the JVM has registered. The names change with the collector: `G1 Young Generation` and `G1 Old Generation` for G1, `ZGC Cycles` and `ZGC Pauses` for ZGC, and so on.
2. **Allocates 10 million small objects.** A long-lived `ArrayList<Point>` plus short-lived peers per slot. Pushes the young generation hard.
3. **Churns 50 MiB through 1 MiB byte buffers.** Each buffer survives one iteration of the loop. With G1 these become humongous regions; with ZGC and Shenandoah they're handled the same as any other large allocation.

The program prints throughput in milliseconds, allocated bytes in MiB, and a per-collector summary of collection counts and total pause time. None of these numbers are absolute statements about the collectors. They are observations of the same workload under different GCs on your machine, and that's exactly the point: try a few, watch the log lines, see what changes.

## What you're meant to notice

- The GC bean names change with the collector.
- The collection counts in the summary change with the collector.
- The GC log line shape changes with the collector. G1 prints region-aware messages; ZGC prints concurrent-cycle messages with sub-millisecond pause durations; Parallel prints classic stop-the-world messages.
- The throughput numbers move within a percent or two for most collectors on this small workload. With a small heap and a fast workload, the differences are real but not dramatic. That is the typical experience. The dramatic differences show up at heap sizes the chapter mentions (16GB+) and at workloads that allocate fast for hours, not seconds.

The flags in the pom are a starting point. The point of the chapter is that GC tuning is mostly about picking the right collector and setting the heap correctly, and that the rest is folklore. Run the demo, watch the log, then go read your own service's GC log.
