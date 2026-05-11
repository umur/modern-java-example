package com.umur.modernjava.ch11;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

/**
 * Demonstrates the JEP 505 (Java 25, fifth preview) structured concurrency API.
 *
 * Run with --enable-preview --release 25.
 *
 * Each demo prints clear "started", "finished", "result", or "cancelled" markers
 * so the cancellation and success flow is observable from stdout.
 */
public final class StructuredConcurrencyExamples {

    public static void main(String[] args) throws Exception {
        section("1. Fan-out with fail-fast (Joiner.awaitAllSuccessfulOrThrow)");
        fanOutAllSuccess();

        section("2. Race-the-fastest (Joiner.anySuccessfulResultOrThrow)");
        raceTheFastest();

        section("3. Open scope (Joiner.awaitAll), collect every result");
        openScopeCollectAll();

        section("4. Failing fork cancels siblings (fail-fast policy)");
        failingForkCancelsSiblings();

        log("ALL DONE");
    }

    /**
     * Demo 1: three simulated I/O calls in parallel. All three succeed.
     * Wall time tracks the slowest fork, not the sum.
     */
    static void fanOutAllSuccess() throws Exception {
        long start = System.currentTimeMillis();
        try (var scope = StructuredTaskScope.open(Joiner.<String>awaitAllSuccessfulOrThrow())) {
            Subtask<String> user = scope.fork(() -> simulateCall("fetchUser", 200));
            Subtask<String> orders = scope.fork(() -> simulateCall("fetchOrders", 350));
            Subtask<String> recs = scope.fork(() -> simulateCall("fetchRecs", 150));

            scope.join();

            log("result: user=" + user.get()
                    + ", orders=" + orders.get()
                    + ", recs=" + recs.get());
        }
        log("elapsed: " + (System.currentTimeMillis() - start) + " ms");
    }

    /**
     * Demo 2: two replicas of the same call. The fastest wins. The slower one
     * is cancelled the moment the winner returns.
     */
    static void raceTheFastest() throws Exception {
        long start = System.currentTimeMillis();
        try (var scope = StructuredTaskScope.open(Joiner.<String>anySuccessfulResultOrThrow())) {
            scope.fork(() -> simulateCall("primary-replica", 300));
            scope.fork(() -> simulateCall("backup-replica", 100));

            String winner = scope.join();
            log("winner: " + winner);
        }
        log("elapsed: " + (System.currentTimeMillis() - start) + " ms");
    }

    /**
     * Demo 3: open scope. Five tasks fan out. Two of them deliberately fail
     * (id 1 and id 4). The main thread walks every subtask after join() and
     * decides what to do.
     */
    static void openScopeCollectAll() throws Exception {
        try (var scope = StructuredTaskScope.open(Joiner.<String>awaitAll())) {
            List<Subtask<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                final int id = i;
                tasks.add(scope.fork(() -> {
                    if (id == 1 || id == 4) {
                        throw new RuntimeException("shard-" + id + " refused");
                    }
                    return simulateCall("shard-" + id, 100 + id * 50);
                }));
            }

            scope.join();

            for (Subtask<String> t : tasks) {
                if (t.state() == Subtask.State.SUCCESS) {
                    log("collected SUCCESS: " + t.get());
                } else {
                    log("collected FAILED:  " + t.exception().getMessage());
                }
            }
        }
    }

    /**
     * Demo 4: a fail-fast scope where one fork throws after 80 ms. The other
     * two forks are sleeping for 1500 ms; they should be cancelled by the
     * scope's shutdown and react to the interrupt by printing "cancelled".
     * The exception propagates out of scope.join() as FailedException.
     */
    static void failingForkCancelsSiblings() {
        try (var scope = StructuredTaskScope.open(Joiner.<String>awaitAllSuccessfulOrThrow())) {
            scope.fork(() -> simulateCall("longRunningA", 1500));
            scope.fork(() -> simulateCall("longRunningB", 1500));
            scope.fork(() -> {
                Thread.sleep(80);
                log("started failingFork");
                throw new RuntimeException("shipping-service unavailable");
            });

            scope.join();
            log("UNEXPECTED: scope.join() returned without throwing");
        } catch (StructuredTaskScope.FailedException fe) {
            log("scope.join() threw FailedException, cause: "
                    + fe.getCause().getClass().getSimpleName()
                    + " '" + fe.getCause().getMessage() + "'");
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log("main thread interrupted: " + ie);
        }
    }

    /**
     * Simulates an I/O call. Sleeps for the given number of milliseconds and
     * returns a synthetic result string. If interrupted (because the scope
     * shut down), prints "cancelled" and rethrows so the runtime sees the
     * subtask as cancelled.
     */
    static String simulateCall(String name, long durationMs) throws InterruptedException {
        log("started " + name + " (will take " + durationMs + " ms)");
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException ie) {
            log("cancelled " + name);
            throw ie;
        }
        log("finished " + name);
        return name + ":ok";
    }

    static void log(String msg) {
        System.out.println("[" + Thread.currentThread() + "] " + msg);
    }

    static void section(String title) {
        System.out.println();
        System.out.println("==== " + title + " ====");
    }
}
