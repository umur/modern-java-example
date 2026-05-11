# Chapter 10: Virtual Threads

Runnable examples demonstrating virtual threads, the virtual-thread-per-task executor, a fixed-pool comparison, and a pinning demo.

## Prerequisites

- **JDK 25** (Temurin, Zulu, Corretto, Liberica, or Oracle).
- **Maven 3.8+**.

If `java -version` shows older than 25, set `JAVA_HOME` to a Java 25 install. On macOS:

```bash
brew install --cask temurin@25
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
```

Or, if Java 25 is on the Homebrew openjdk path:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
```

## Run

```bash
mvn -q compile exec:java
```

Virtual threads are final in Java 21 and stable in Java 25. No `--enable-preview` flag is needed.

## What you'll see

The program runs four demonstrations and prints observable output for each.

1. **Spawning 100,000 virtual threads.** Each thread sleeps 100 ms, then exits. The wall-clock total is on the order of a few hundred milliseconds, because all the threads sleep concurrently. The same workload on a 200-thread fixed pool would take about 50 seconds.

2. **Virtual executor versus fixed pool.** The same 5,000-task workload run twice: once on `Executors.newVirtualThreadPerTaskExecutor()`, once on `Executors.newFixedThreadPool(100)`. Wall-clock times are printed for both.

3. **Thread identity.** Each spawn prints `Thread.currentThread()` and `Thread.isVirtual()` so you can see the carrier-thread mounting in action. Look for `VirtualThread[#nnn]/runnable@ForkJoinPool-1-worker-X`.

4. **Pinning demo.** A virtual thread blocks once inside a `synchronized` block (which historically pinned its carrier on JDK 21 to 23) and once with a `ReentrantLock` (which does not pin). Both report which carrier they ran on; on JDK 24+, JEP 491 has removed pinning for the synchronized case in most situations, so the trace flag is the surer diagnosis.

To see pinning explicitly when it does happen, run with the diagnostic flag:

```bash
mvn -q compile exec:java -Dexec.args="" \
  -Djdk.tracePinnedThreads=full
```

(This works because `exec:java` runs in the same JVM that Maven uses for the exec-plugin fork; if you don't see pinning traces, your JDK has already closed those pinning cases through JEP 491.)
