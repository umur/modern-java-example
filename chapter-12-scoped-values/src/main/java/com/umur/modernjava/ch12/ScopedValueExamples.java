package com.umur.modernjava.ch12;

import java.util.NoSuchElementException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

/**
 * Demonstrates the JEP 506 (Java 25, final) ScopedValue API.
 *
 * Note: ScopedValue itself is final in Java 25 and needs no flag. This project
 * enables preview because demo 2 also uses StructuredTaskScope, which is still
 * preview as of Java 25 (JEP 505, fifth preview).
 *
 * Each demo prints the value seen at each frame so the dynamic-extent semantics
 * are visible from stdout.
 */
public final class ScopedValueExamples {

    /** Per-request trace id. Bound at the entry point, read from anywhere downstream. */
    public static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

    /** Per-request user locale. Bound alongside REQUEST_ID. */
    public static final ScopedValue<String> LOCALE = ScopedValue.newInstance();

    public static void main(String[] args) throws Exception {
        section("1. Basic binding and read (no preview required)");
        demoBasicBinding();

        section("2. Fork inheritance with StructuredTaskScope (preview)");
        demoForkInheritance();

        section("3. Reading an unbound ScopedValue throws (no preview required)");
        demoUnboundRead();

        section("4. Rebinding inside a sub-scope (no preview required)");
        demoRebinding();

        log("ALL DONE");
    }

    /**
     * Demo 1: bind once at the entry point, read from a method three frames deep.
     * REQUEST_ID never appears as a parameter on any inner method.
     */
    static void demoBasicBinding() {
        ScopedValue.where(REQUEST_ID, "req-abc-123")
                   .where(LOCALE, "en-US")
                   .run(() -> {
                       log("entry: REQUEST_ID=" + REQUEST_ID.get()
                               + ", LOCALE=" + LOCALE.get());
                       outerService();
                   });
        log("after run: REQUEST_ID.isBound()=" + REQUEST_ID.isBound());
    }

    static void outerService() {
        log("outerService: REQUEST_ID=" + REQUEST_ID.get());
        innerService();
    }

    static void innerService() {
        log("innerService: REQUEST_ID=" + REQUEST_ID.get()
                + " (three frames deep, never passed)");
    }

    /**
     * Demo 2: open a StructuredTaskScope inside a binding. Each fork is a virtual
     * thread; each fork reads the parent's REQUEST_ID without any explicit
     * propagation. This is the contract between scoped values and structured
     * concurrency.
     */
    static void demoForkInheritance() throws Exception {
        ScopedValue.where(REQUEST_ID, "req-fanout-456").run(() -> {
            log("parent: REQUEST_ID=" + REQUEST_ID.get());
            try (var scope = StructuredTaskScope.open(
                    Joiner.<String>awaitAllSuccessfulOrThrow())) {
                Subtask<String> billing = scope.fork(() -> callService("billing"));
                Subtask<String> shipping = scope.fork(() -> callService("shipping"));
                Subtask<String> recs = scope.fork(() -> callService("recs"));

                scope.join();

                log("results: " + billing.get() + ", " + shipping.get() + ", " + recs.get());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log("main interrupted: " + ie);
            }
        });
    }

    static String callService(String name) throws InterruptedException {
        // Each fork is a virtual thread. It can read REQUEST_ID without ever
        // having been passed the value.
        log("fork '" + name + "' on " + Thread.currentThread()
                + " reads REQUEST_ID=" + REQUEST_ID.get());
        Thread.sleep(50);
        return name + ":ok";
    }

    /**
     * Demo 3: calling .get() outside any binding throws NoSuchElementException.
     * Demonstrates fail-loud behaviour. We catch and print so the program
     * continues to the next demo.
     */
    static void demoUnboundRead() {
        log("REQUEST_ID.isBound()=" + REQUEST_ID.isBound());
        try {
            String v = REQUEST_ID.get();
            log("UNEXPECTED: got value '" + v + "' when no binding exists");
        } catch (NoSuchElementException nse) {
            log("expected: NoSuchElementException -> '" + nse.getMessage() + "'");
        }

        // .orElse(...) is the supported way to express "value if bound, else default".
        String fallback = REQUEST_ID.orElse("(no-request-id)");
        log("REQUEST_ID.orElse(...) = '" + fallback + "'");
    }

    /**
     * Demo 4: a nested where(KEY, newValue).run(...) overrides the outer value
     * for the dynamic extent of the inner block. The outer value comes back when
     * the inner block exits. No try/finally required.
     */
    static void demoRebinding() {
        ScopedValue.where(REQUEST_ID, "outer").run(() -> {
            log("outer: REQUEST_ID=" + REQUEST_ID.get());

            ScopedValue.where(REQUEST_ID, "outer.sub").run(() -> {
                log("inner: REQUEST_ID=" + REQUEST_ID.get());
            });

            log("back to outer: REQUEST_ID=" + REQUEST_ID.get());
        });
    }

    static void log(String msg) {
        System.out.println("[" + Thread.currentThread() + "] " + msg);
    }

    static void section(String title) {
        System.out.println();
        System.out.println("==== " + title + " ====");
    }
}
