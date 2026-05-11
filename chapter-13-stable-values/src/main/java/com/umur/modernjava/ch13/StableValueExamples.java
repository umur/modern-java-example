package com.umur.modernjava.ch13;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates the JEP 502 (Java 25, first preview) StableValue API.
 *
 * Requires --enable-preview on both compile and run. The pom.xml wires the
 * flag into the maven-compiler-plugin and the exec-maven-plugin so
 * `mvn -q clean compile exec:exec` runs without further configuration.
 *
 * Each demo prints `init` markers to make the at-most-once guarantee visible
 * from stdout, and `read` markers to show how reads are served from the
 * already-initialised holder on subsequent calls.
 */
public final class StableValueExamples {

    public static void main(String[] args) throws Exception {
        section("1. Per-instance lazy field on a TaxCalculator");
        demoPerInstanceField();

        section("2. Race-safe initialisation: 20 virtual threads, supplier runs once");
        demoRaceSafety();

        section("3. Stable list: 256-slot lazy array, only requested slots initialise");
        demoStableList();

        section("4. Stable map: fixed-key lazy registry of TaxCalculators");
        demoStableMap();

        log("ALL DONE");
    }

    /**
     * Demo 1: A TaxCalculator holds a StableValue<Map<String, BigDecimal>>.
     * The expensive load (printed as "init: ...") runs at most once per instance.
     * Subsequent reads on the same instance hit the populated holder.
     */
    static void demoPerInstanceField() {
        TaxCalculator eu = new TaxCalculator("EU");
        log("first  read EU/electronics: " + eu.rateFor("electronics"));
        log("second read EU/groceries:   " + eu.rateFor("groceries"));
        log("third  read EU/services:    " + eu.rateFor("services"));

        // A different instance has its own, separate stable value.
        TaxCalculator us = new TaxCalculator("US");
        log("first  read US/electronics: " + us.rateFor("electronics"));
    }

    /**
     * Demo 2: Twenty virtual threads call orElseSet on the same fresh stable
     * value. The supplier increments a counter; we then assert the counter
     * advanced to exactly one. The runtime serialises first-write under the
     * hood, so losers of the race read the winner's value without re-running
     * the supplier.
     */
    static void demoRaceSafety() throws InterruptedException {
        StableValue<String> v = StableValue.of();
        AtomicInteger supplierCalls = new AtomicInteger();
        int threads = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            int id = i;
            Thread.ofVirtual().start(() -> {
                try {
                    start.await();
                    String value = v.orElseSet(() -> {
                        supplierCalls.incrementAndGet();
                        log("init: supplier running on " + Thread.currentThread());
                        sleep(20);
                        return "loaded-once";
                    });
                    log("thread " + id + " read: " + value);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        // Release them all at the same instant.
        start.countDown();
        done.await();

        log("supplier ran " + supplierCalls.get() + " time(s) for " + threads + " threads");
        log("final value: " + v.orElse("(unset)"));
    }

    /**
     * Demo 3: A 256-slot stable list. The IntFunction is the per-slot
     * initialiser. We read slots 5, 42, and 200; the initialiser runs three
     * times, once per requested slot. The other 253 slots stay unset.
     */
    static void demoStableList() {
        AtomicInteger inits = new AtomicInteger();
        List<String> items = StableValue.list(256, slot -> {
            inits.incrementAndGet();
            log("init: slot " + slot);
            return "item-" + slot;
        });

        log("read slot 5:   " + items.get(5));
        log("read slot 42:  " + items.get(42));
        log("read slot 200: " + items.get(200));
        log("re-read slot 42 (no init this time): " + items.get(42));
        log("initialiser ran " + inits.get() + " time(s) across the four reads");
    }

    /**
     * Demo 4: A stable map keyed on region codes. Each value is a
     * TaxCalculator constructed on first read of that key. Reading "EU" does
     * not construct the LATAM calculator. Re-reading "EU" returns the same
     * instance.
     */
    static void demoStableMap() {
        Set<String> regions = Set.of("EU", "US", "APAC", "LATAM");
        AtomicInteger ctorCalls = new AtomicInteger();

        Map<String, TaxCalculator> calculators = StableValue.map(regions, region -> {
            ctorCalls.incrementAndGet();
            log("init: constructing TaxCalculator for " + region);
            return new TaxCalculator(region);
        });

        TaxCalculator eu1 = calculators.get("EU");
        TaxCalculator eu2 = calculators.get("EU");
        TaxCalculator us = calculators.get("US");

        log("EU calculator returns: " + eu1.rateFor("electronics"));
        log("US calculator returns: " + us.rateFor("electronics"));
        log("eu1 == eu2 (same instance)? " + (eu1 == eu2));
        log("constructor ran " + ctorCalls.get() + " time(s); LATAM and APAC never built");
    }

    // ---------- helpers ----------

    static void log(String msg) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + msg);
    }

    static void section(String title) {
        System.out.println();
        System.out.println("==== " + title + " ====");
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * The motivating shape from the chapter prose: a tax calculator whose
     * fifty-millisecond rate-table load is hidden behind a per-instance
     * StableValue. The first call to rateFor pays the load; every later call
     * is a map lookup.
     */
    static final class TaxCalculator {

        private final String regionCode;
        private final StableValue<Map<String, BigDecimal>> rates = StableValue.of();

        TaxCalculator(String regionCode) {
            this.regionCode = regionCode;
        }

        BigDecimal rateFor(String productCategory) {
            Map<String, BigDecimal> r = rates.orElseSet(this::loadRatesFromCsv);
            return r.getOrDefault(productCategory, BigDecimal.ZERO);
        }

        private Map<String, BigDecimal> loadRatesFromCsv() {
            log("init: loading rate table for region=" + regionCode + " (50ms)");
            sleep(50);
            Map<String, BigDecimal> table = new HashMap<>();
            switch (regionCode) {
                case "EU" -> {
                    table.put("electronics", new BigDecimal("0.21"));
                    table.put("groceries", new BigDecimal("0.05"));
                    table.put("services", new BigDecimal("0.21"));
                }
                case "US" -> {
                    table.put("electronics", new BigDecimal("0.07"));
                    table.put("groceries", new BigDecimal("0.00"));
                    table.put("services", new BigDecimal("0.06"));
                }
                case "APAC" -> {
                    table.put("electronics", new BigDecimal("0.10"));
                    table.put("groceries", new BigDecimal("0.05"));
                    table.put("services", new BigDecimal("0.08"));
                }
                case "LATAM" -> {
                    table.put("electronics", new BigDecimal("0.16"));
                    table.put("groceries", new BigDecimal("0.04"));
                    table.put("services", new BigDecimal("0.16"));
                }
                default -> {
                    // No-op; getOrDefault will return zero.
                }
            }
            return Map.copyOf(table);
        }
    }
}
