package com.umur.modernjava.ch10;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class VirtualThreadExamples {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Modern Java in Practice, Chapter 10: Virtual Threads ===");
        System.out.println("JDK: " + Runtime.version());
        System.out.println();

        demoSpawnHundredThousand();
        System.out.println();

        demoVirtualVsFixedPool();
        System.out.println();

        demoThreadIdentity();
        System.out.println();

        demoPinning();
    }

    // 1. Spawn 100,000 virtual threads, each sleeping briefly.
    //    The same workload on a 200-thread fixed pool would take ~50 seconds.
    static void demoSpawnHundredThousand() throws InterruptedException {
        System.out.println("--- Demo 1: 100,000 virtual threads, each sleeping 100 ms ---");
        var counter = new AtomicInteger();
        long start = System.nanoTime();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 100_000).forEach(i -> executor.submit(() -> {
                try {
                    Thread.sleep(Duration.ofMillis(100));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                counter.incrementAndGet();
            }));
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("  completed " + counter.get() + " tasks in " + elapsedMs + " ms");
    }

    // 2. Same workload, two executors. The fixed pool throughput is capped by pool size.
    static void demoVirtualVsFixedPool() throws InterruptedException {
        int taskCount = 5_000;
        long sleepMs = 50;
        System.out.println("--- Demo 2: " + taskCount + " I/O-bound tasks, virtual vs fixed pool(100) ---");

        long virtualMs = runTasks(Executors.newVirtualThreadPerTaskExecutor(), taskCount, sleepMs);
        System.out.println("  virtual-thread-per-task : " + virtualMs + " ms");

        long fixedMs = runTasks(Executors.newFixedThreadPool(100), taskCount, sleepMs);
        System.out.println("  fixedThreadPool(100)    : " + fixedMs + " ms");

        System.out.println("  speedup                 : " + String.format("%.1fx", (double) fixedMs / virtualMs));
    }

    static long runTasks(ExecutorService executor, int taskCount, long sleepMs) {
        long start = System.nanoTime();
        try (executor) {
            IntStream.range(0, taskCount).forEach(i -> executor.submit(() -> {
                try {
                    Thread.sleep(Duration.ofMillis(sleepMs));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        return (System.nanoTime() - start) / 1_000_000;
    }

    // 3. Show that Thread.currentThread() inside a virtual thread reports it as virtual,
    //    and prints the carrier thread it's currently mounted on.
    static void demoThreadIdentity() throws InterruptedException {
        System.out.println("--- Demo 3: Thread identity (virtual vs platform) ---");
        var platform = Thread.ofPlatform().name("platform-demo").start(() -> {
            var t = Thread.currentThread();
            System.out.println("  on platform thread: " + t + ", isVirtual=" + t.isVirtual());
        });
        platform.join();

        var virtual = Thread.ofVirtual().name("virtual-demo").start(() -> {
            var t = Thread.currentThread();
            System.out.println("  on virtual thread:  " + t + ", isVirtual=" + t.isVirtual());
        });
        virtual.join();
    }

    // 4. Pinning demo: same blocking work under synchronized vs ReentrantLock.
    //    Run with -Djdk.tracePinnedThreads=full to see actual pin events.
    //    On JDK 24+, JEP 491 has closed most synchronized pinning cases.
    static void demoPinning() throws InterruptedException {
        System.out.println("--- Demo 4: synchronized vs ReentrantLock under blocking I/O ---");
        var monitor = new Object();
        var lock = new ReentrantLock();

        var underSync = Thread.ofVirtual().name("sync-victim").start(() -> {
            synchronized (monitor) {
                System.out.println("  in synchronized block: " + Thread.currentThread());
                sleepBriefly();
            }
        });
        underSync.join();

        var underLock = Thread.ofVirtual().name("lock-victim").start(() -> {
            lock.lock();
            try {
                System.out.println("  in ReentrantLock:      " + Thread.currentThread());
                sleepBriefly();
            } finally {
                lock.unlock();
            }
        });
        underLock.join();

        System.out.println("  (run again with -Djdk.tracePinnedThreads=full to see pinning traces)");
    }

    static void sleepBriefly() {
        try {
            Thread.sleep(Duration.ofMillis(50));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
