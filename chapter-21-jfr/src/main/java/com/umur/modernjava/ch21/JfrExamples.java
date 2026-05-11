package com.umur.modernjava.ch21;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.consumer.RecordingStream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Walks the three JFR APIs covered in Chapter 21.
 *
 * <ol>
 *   <li>The JVM starts a 10-second recording from the {@code -XX:StartFlightRecording}
 *       flag set in the pom. This program does not call the {@code Recording} API
 *       directly; the JVM owns that recording and dumps it to {@code demo.jfr} on exit.
 *   <li>A {@link RecordingStream} subscribes to live {@code jdk.GarbageCollection}
 *       events and prints each pause as the JVM completes it.
 *   <li>A small simulated workload allocates objects, processes synthetic orders,
 *       and emits a custom {@link OrderProcessedEvent} for each one. The events
 *       land in the same {@code demo.jfr} file alongside the built-in JVM events.
 * </ol>
 */
public final class JfrExamples {

    private static final int ORDER_COUNT = 200_000;
    private static final Random RANDOM = new Random(42L);

    private JfrExamples() {
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("[JFR] Recording started by -XX:StartFlightRecording flag (filename=demo.jfr, settings=profile, duration=10s)");

        AtomicInteger gcCount = new AtomicInteger();
        try (RecordingStream stream = new RecordingStream()) {
            stream.enable("jdk.GarbageCollection").withoutThreshold();   // (1)
            stream.onEvent("jdk.GarbageCollection", event -> {           // (2)
                int n = gcCount.incrementAndGet();
                String cause = event.getString("cause");
                Duration duration = event.getDuration();
                System.out.printf("[Stream] GC #%d: cause=%s, duration=%.3fms%n",
                        n, cause, duration.toNanos() / 1_000_000.0);
            });
            stream.startAsync();                                          // (3)
            System.out.println("[Stream] Subscribed to jdk.GarbageCollection events");

            processOrders();

            // Give the streaming thread a moment to drain any final events.
            Thread.sleep(500);
        }

        System.out.println();
        System.out.println("Done. JFR file written to demo.jfr; analyze with `jfr print demo.jfr` or open in JDK Mission Control.");
        System.out.println("Try: jfr summary demo.jfr");
        System.out.println("Try: jfr print --events com.umur.modernjava.OrderProcessed demo.jfr");
    }

    /**
     * Simulates a small order-processing workload. For each order:
     * <ul>
     *   <li>Allocates and discards a chunk of memory to give the GC something to do.</li>
     *   <li>Spends a small amount of CPU time on synthetic work.</li>
     *   <li>Emits a custom {@link OrderProcessedEvent} carrying the order's identifying fields.</li>
     * </ul>
     */
    private static void processOrders() {
        System.out.printf("Processing %,d simulated orders...%n", ORDER_COUNT);
        long t0 = System.nanoTime();

        long checksum = 0L;
        for (int i = 0; i < ORDER_COUNT; i++) {
            String orderId = "order-" + i;
            String customerId = "customer-" + (i % 200);
            long totalCents = 1000L + RANDOM.nextInt(50_000);

            OrderProcessedEvent event = new OrderProcessedEvent();
            event.begin();                                                // (4)
            try {
                checksum += simulateOrderWork(orderId, totalCents);

                event.orderId    = orderId;
                event.customerId = customerId;
                event.totalCents = totalCents;
            } finally {
                event.commit();                                           // (5)
            }
        }

        long t1 = System.nanoTime();
        System.out.printf("Processed %,d orders in %,d ms (checksum=%d)%n",
                ORDER_COUNT, (t1 - t0) / 1_000_000, checksum);
    }

    /**
     * Allocates a few short-lived buffers and a small persistent record so the GC has work to do
     * and the JFR allocation samplers have something to sample. Returns a checksum so the JIT
     * can't elide the work.
     */
    private static long simulateOrderWork(String orderId, long totalCents) {
        // Short-lived allocations: young-gen pressure that turns into real GC work.
        byte[] payload = new byte[16 * 1024];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) ((orderId.hashCode() + i) & 0xFF);
        }

        // A small list of strings, mostly short-lived, with some surviving into old gen.
        List<String> lines = new ArrayList<>(16);
        for (int i = 0; i < 16; i++) {
            lines.add(orderId + "-line-" + i + "-" + (totalCents / (i + 1)));
        }

        long sum = 0L;
        for (String line : lines) {
            sum += line.length();
            sum += line.hashCode() & 0xFF;
        }
        for (int i = 0; i < payload.length; i += 64) {
            sum += payload[i];
        }
        return sum;
    }

    /**
     * Custom JFR event recorded for every order processed. Same machinery as the built-in
     * JVM events: appears in the same {@code .jfr} file, opens in JDK Mission Control, and
     * is consumable via {@link RecordingStream}.
     */
    @Name("com.umur.modernjava.OrderProcessed")                           // (6)
    @Label("Order Processed")
    @Description("An order moved from PLACED to SHIPPED")
    @Category({"Application", "Orders"})
    public static class OrderProcessedEvent extends Event {
        @Label("Order ID")
        public String orderId;

        @Label("Customer ID")
        public String customerId;

        @Label("Total (cents)")
        public long totalCents;
    }

    // (1) `enable` returns event settings; `withoutThreshold` says "deliver every event".
    // (2) The handler runs on a JFR-internal thread. Keep the work small.
    // (3) `startAsync` lets the workload run on the main thread while events stream in the background.
    // (4) `begin` records the event start time so `commit` can fill in the duration.
    // (5) `commit` writes into the JFR buffer. No-op if no recording is active.
    // (6) `@Name` is the stable identifier. Reverse-DNS plus CamelCase, same shape as `jdk.GarbageCollection`.
}
