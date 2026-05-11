package com.umur.modernjava.ch15;

import java.util.List;
import java.util.Objects;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;

/**
 * Demonstrates JEP 485 (Java 24, final) stream gatherers.
 *
 * No --enable-preview required. Builds and runs on JDK 24 or later. The pom in
 * this project targets release 25 to match the rest of the book.
 *
 * Each demo prints labelled input/output markers so the gatherer's effect on
 * the stream is visible from stdout.
 */
public final class GathererExamples {

    public static void main(String[] args) {
        section("1. Gatherers.windowFixed(3) over ten integers");
        demoWindowFixed();

        section("2. Gatherers.windowSliding(3) over six integers");
        demoWindowSliding();

        section("3. Gatherers.fold(...) joins strings into a single value");
        demoFold();

        section("4. Gatherers.scan(...) emits a running prefix sum");
        demoScan();

        section("5. A custom dedupAdjacent() gatherer");
        demoDedupAdjacent();

        log("ALL DONE");
    }

    /**
     * Demo 1: windowFixed chunks the input into non-overlapping batches. The
     * trailing partial window is flushed by the gatherer's finisher, so 10
     * integers with windowSize 3 produce four lists: three full and one with
     * the leftover element.
     */
    static void demoWindowFixed() {
        List<Integer> input = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        log("input:  " + input);

        List<List<Integer>> windows = input.stream()
                .gather(Gatherers.windowFixed(3))
                .toList();

        log("output: " + windows);
        log("count:  " + windows.size() + " windows (3 full + 1 partial)");
    }

    /**
     * Demo 2: windowSliding emits every consecutive window of the given size.
     * Six inputs with windowSize 3 produce four overlapping windows. No
     * partial window at the end; sliding stops once it can no longer fill.
     */
    static void demoWindowSliding() {
        List<Integer> input = List.of(1, 2, 3, 4, 5, 6);
        log("input:  " + input);

        List<List<Integer>> windows = input.stream()
                .gather(Gatherers.windowSliding(3))
                .toList();

        log("output: " + windows);
        log("count:  " + windows.size() + " sliding windows of 3");
    }

    /**
     * Demo 3: fold reduces the stream and emits the final accumulator as a
     * one-element stream. The use case is composition; if you only want the
     * value, Stream::reduce is shorter, but fold is what plugs into the rest
     * of a gather pipeline.
     */
    static void demoFold() {
        List<String> input = List.of("alpha", "beta", "gamma", "delta");
        log("input:  " + input);

        String joined = input.stream()
                .gather(Gatherers.fold(
                        () -> "",
                        (acc, s) -> acc.isEmpty() ? s : acc + ", " + s))
                .findFirst()
                .orElse("");

        log("output: \"" + joined + "\"");
    }

    /**
     * Demo 4: scan emits the running accumulator after each input. Five
     * integers produce five running sums (1, 3, 6, 10, 15), one per input.
     */
    static void demoScan() {
        List<Integer> input = List.of(1, 2, 3, 4, 5);
        log("input:  " + input);

        List<Integer> prefixSums = input.stream()
                .gather(Gatherers.scan(() -> 0, Integer::sum))
                .toList();

        log("output: " + prefixSums + "  (running prefix sums)");
    }

    /**
     * Demo 5: a custom gatherer. dedupAdjacent collapses consecutive equal
     * elements; non-adjacent duplicates are preserved. Note [1, 1, 2, 2, 2,
     * 3, 1, 1, 1, 4] becomes [1, 2, 3, 1, 4]: the second run of 1s is kept
     * because it's separated from the first by a 2 and a 3.
     */
    static void demoDedupAdjacent() {
        List<Integer> input = List.of(1, 1, 2, 2, 2, 3, 1, 1, 1, 4);
        log("input:  " + input);

        List<Integer> deduped = input.stream()
                .gather(dedupAdjacent())
                .toList();

        log("output: " + deduped + "  (adjacent runs collapsed)");
    }

    /**
     * A custom gatherer factory: emit each element only if it differs from the
     * previously emitted one. Sequential-only because adjacency loses meaning
     * across parallel splits. Twelve lines of logic plus the inner State
     * class.
     */
    static <T> Gatherer<T, ?, T> dedupAdjacent() {
        class State {
            T last;
            boolean hasLast;
        }
        return Gatherer.ofSequential(
                State::new,
                (state, element, downstream) -> {
                    if (!state.hasLast || !Objects.equals(state.last, element)) {
                        state.last = element;
                        state.hasLast = true;
                        return downstream.push(element);
                    }
                    return !downstream.isRejecting();
                });
    }

    // ---------- helpers ----------

    static void log(String msg) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + msg);
    }

    static void section(String title) {
        System.out.println();
        System.out.println("==== " + title + " ====");
    }
}
