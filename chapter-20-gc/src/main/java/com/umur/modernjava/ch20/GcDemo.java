package com.umur.modernjava.ch20;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 * Allocation-heavy demo that exercises the active garbage collector.
 *
 * <p>The point of this program is not to do anything useful. The point is to allocate enough
 * objects, fast enough, that the GC has to run and the GC log says something interesting.
 * Run with different collector flags to see the same workload through different eyes:
 *
 * <pre>{@code
 *   mvn -q clean compile exec:exec                                # G1 (the project default)
 *   mvn -q exec:exec -Dexec.args="-Xmx512m -Xms512m -XX:+UseZGC -Xlog:gc"
 *   mvn -q exec:exec -Dexec.args="-Xmx512m -Xms512m -XX:+UseShenandoahGC -Xlog:gc"
 *   mvn -q exec:exec -Dexec.args="-Xmx512m -Xms512m -XX:+UseParallelGC -Xlog:gc"
 *   mvn -q exec:exec -Dexec.args="-Xmx512m -Xms512m -XX:+UseSerialGC -Xlog:gc"
 * }</pre>
 *
 * <p>The throughput numbers and the GC log lines change with the collector. The application
 * output stays the same.
 */
public final class GcDemo {

    private static final int SMALL_OBJECTS = 10_000_000;
    private static final int LARGE_BUFFER_COUNT = 50;
    private static final int LARGE_BUFFER_SIZE_BYTES = 1 << 20; // 1 MiB

    private GcDemo() {
    }

    public static void main(String[] args) {
        printActiveGc();

        long t0 = System.nanoTime();
        long allocatedSmall = allocateSmallObjects();
        long t1 = System.nanoTime();
        long allocatedLargeBytes = churnLargeBuffers();
        long t2 = System.nanoTime();

        System.out.printf("Allocated %,d small objects in %,d ms%n",
                allocatedSmall, (t1 - t0) / 1_000_000);
        System.out.printf("Churned %,d MiB through %,d large byte[] buffers in %,d ms%n",
                (LARGE_BUFFER_COUNT * (long) LARGE_BUFFER_SIZE_BYTES) >> 20,
                LARGE_BUFFER_COUNT,
                (t2 - t1) / 1_000_000);
        System.out.printf("Total wall time: %,d ms%n", (t2 - t0) / 1_000_000);
        System.out.printf("Total allocated bytes (rough): %,d MiB%n",
                (allocatedLargeBytes + allocatedSmall * 16L) >> 20);

        printGcSummary();
    }

    /** Build a long-lived list of millions of tiny objects, with a few short-lived peers per slot. */
    private static long allocateSmallObjects() {
        List<Point> survivors = new ArrayList<>(SMALL_OBJECTS);
        for (int i = 0; i < SMALL_OBJECTS; i++) {
            // One survivor and a few short-lived peers, so the young generation actually has work.
            survivors.add(new Point(i, -i));
            Point dead1 = new Point(i + 1, i + 2);
            Point dead2 = new Point(i + 3, i + 4);
            // touch the short-lived ones so the JIT can't elide them
            if (dead1.x() == Integer.MIN_VALUE && dead2.y() == Integer.MIN_VALUE) {
                System.out.println("unreachable");
            }
        }
        return survivors.size();
    }

    /** Allocate and discard a steady stream of 1 MiB buffers to push humongous/old-gen pressure. */
    private static long churnLargeBuffers() {
        long totalBytes = 0L;
        byte[] sink = new byte[16];
        for (int i = 0; i < LARGE_BUFFER_COUNT; i++) {
            byte[] buffer = new byte[LARGE_BUFFER_SIZE_BYTES];
            buffer[0] = (byte) i;
            buffer[buffer.length - 1] = (byte) (i ^ 0x5A);
            sink[i % sink.length] = buffer[0];
            totalBytes += buffer.length;
        }
        // keep sink reachable so the JIT keeps the writes
        if (sink[0] == sink[1] && sink[2] == sink[3] && sink[4] == sink[5]
                && sink[6] == sink[7] && sink[8] == sink[9] && sink[10] == sink[11]) {
            System.out.println("rare");
        }
        return totalBytes;
    }

    private static void printActiveGc() {
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        System.out.print("Active GC bean(s): ");
        for (int i = 0; i < beans.size(); i++) {
            if (i > 0) {
                System.out.print(", ");
            }
            System.out.print(beans.get(i).getName());
        }
        System.out.println();
    }

    private static void printGcSummary() {
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        long totalCollections = 0L;
        long totalMillis = 0L;
        for (GarbageCollectorMXBean bean : beans) {
            long count = bean.getCollectionCount();
            long millis = bean.getCollectionTime();
            System.out.printf("GC '%s': %,d collections, %,d ms total%n",
                    bean.getName(), count, millis);
            if (count > 0) {
                totalCollections += count;
            }
            if (millis > 0) {
                totalMillis += millis;
            }
        }
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        System.out.printf("Heap used: %,d MiB / committed %,d MiB / max %,d MiB%n",
                heap.getUsed() >> 20, heap.getCommitted() >> 20, heap.getMax() >> 20);
        System.out.printf("GC total: %,d collections, %,d ms%n", totalCollections, totalMillis);
    }

    /** Tiny record used as small-object filler. Two ints plus the record header. */
    private record Point(int x, int y) {
    }
}
