package com.umur.modernjava.ch22;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The chapter-22 demo application. Deliberately small.
 *
 * <p>The point of this program is not the program; it's the build and distribution
 * pipeline that wraps it. The README walks through using this binary with AppCDS,
 * jlink, and jpackage. The code below exists so that pipeline has something to run.</p>
 *
 * <p>To make the runtime-image story interesting, the program touches a handful of
 * JDK modules: {@code java.base} (always), {@code java.logging} (the logger), and
 * {@code java.net.http} (the HttpClient). That gives {@code jdeps --print-module-deps}
 * a non-trivial answer to print and gives {@code jlink} a non-trivial set of modules
 * to wire together.</p>
 */
public final class HelloApp {

    private static final Logger LOG = Logger.getLogger(HelloApp.class.getName());

    private HelloApp() {
    }

    public static void main(String[] args) {
        long t0 = System.nanoTime();

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();          // (1)
        long jvmUptimeMs = runtime.getUptime();
        String jvmName = runtime.getVmName() + " " + runtime.getVmVersion();

        // Touch java.net.http so jdeps reports it. We don't actually call the network,
        // so this stays a fast, deterministic offline demo.
        HttpClient client = HttpClient.newBuilder()                            // (2)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        URI demoUri = URI.create("https://example.test/never-fetched");

        LOG.log(Level.INFO, "Hello, modern Java");                             // (3)

        long t1 = System.nanoTime();
        long elapsedUs = (t1 - t0) / 1_000;

        System.out.println();
        System.out.println("=== Chapter 22: Startup and Distribution ===");
        System.out.printf("JVM:               %s%n", jvmName);
        System.out.printf("JVM uptime at main:%,d ms%n", jvmUptimeMs);
        System.out.printf("main() body took:  %,d us%n", elapsedUs);
        System.out.printf("HttpClient class:  %s%n", client.getClass().getName());
        System.out.printf("(Demo URI prepared but not fetched: %s)%n", demoUri);
        System.out.println();

        if (args.length > 0 && List.of(args).contains("--bench")) {            // (4)
            runMicroBench();
        }

        System.out.println("Hello, modern Java");
    }

    /**
     * Tiny CPU-only loop to give the JIT something to compile, useful when comparing
     * cold-start runs against AppCDS-warmed runs.
     */
    private static void runMicroBench() {
        long t0 = System.nanoTime();
        long acc = 0;
        for (int i = 0; i < 1_000_000; i++) {
            acc += (i * 31L) ^ (i >>> 3);
        }
        long t1 = System.nanoTime();
        System.out.printf("Bench: acc=%d, took %,d us%n", acc, (t1 - t0) / 1_000);
    }

    // (1) RuntimeMXBean reports JVM uptime measured from the start of the JVM process,
    //     which is closer to the real "cold start" number than nanoTime() inside main().
    // (2) HttpClient is in java.net.http. Touching it forces jdeps to include the module.
    // (3) java.util.logging is in java.logging. One more module for the runtime image.
    // (4) Pass --bench on the command line to also do a small CPU loop, useful when
    //     comparing AppCDS or AOT runs against an unwarmed baseline.
}
