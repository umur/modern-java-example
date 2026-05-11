package com.umur.modernjava.ch14;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.UUID;

/**
 * Demonstrates JEP 431 (Java 21, final) Sequenced Collections.
 *
 * No --enable-preview required. Builds and runs on JDK 21 or later. The pom in
 * this project targets release 25 to match the rest of the book.
 *
 * Each demo prints labelled before/after markers so the new contracts are
 * visible from stdout, especially the view-not-copy semantics of reversed().
 */
public final class SequencedCollectionExamples {

    public static void main(String[] args) {
        section("1. LinkedHashMap: first/last entry without the iterator dance");
        demoLinkedHashMapAccess();

        section("2. List.reversed() is a view, not a copy");
        demoReversedIsAView();

        section("3. LinkedHashSet: getFirst, getLast, addFirst moves duplicates");
        demoLinkedHashSet();

        section("4. A method that takes SequencedCollection<T>");
        demoWiderParameterType();

        log("ALL DONE");
    }

    /**
     * Demo 1: a LinkedHashMap of recent orders, ordered by insertion. The old
     * way to read the first entry was map.entrySet().iterator().next(). The
     * new way is map.firstEntry(). The "ten most recent" pattern collapses
     * an iterate-with-counter loop into a stream over the reversed view.
     */
    static void demoLinkedHashMapAccess() {
        LinkedHashMap<UUID, Order> recentOrders = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            UUID id = UUID.nameUUIDFromBytes(("order-" + i).getBytes());
            recentOrders.put(id, new Order(id, "customer-" + i, i * 10));
        }
        log("recentOrders has " + recentOrders.size() + " entries");

        // Old: recentOrders.entrySet().iterator().next();
        Map.Entry<UUID, Order> oldest = recentOrders.firstEntry();
        log("firstEntry (oldest):  " + oldest.getValue());

        Map.Entry<UUID, Order> newest = recentOrders.lastEntry();
        log("lastEntry  (newest):  " + newest.getValue());

        // The "ten most recent" endpoint, written as a single fluent expression.
        List<Order> top10 = recentOrders.reversed()
                .sequencedValues()
                .stream()
                .limit(10)
                .toList();
        log("top 10 most recent (reversed view + stream limit):");
        top10.forEach(order -> log("  " + order));

        // pollFirstEntry removes and returns. Mutating operation; the map shrinks.
        Map.Entry<UUID, Order> popped = recentOrders.pollFirstEntry();
        log("pollFirstEntry popped: " + popped.getValue());
        log("recentOrders now has " + recentOrders.size() + " entries");
    }

    /**
     * Demo 2: the single most important behavioural fact in this chapter.
     * reversed() returns a view backed by the original list. Mutations
     * propagate both ways. We mutate the original after taking the view, then
     * mutate through the view back into the original, printing each step so
     * the live-view contract is unambiguous.
     */
    static void demoReversedIsAView() {
        List<Integer> numbers = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        List<Integer> rev = numbers.reversed();

        log("numbers (start): " + numbers);
        log("rev     (start): " + rev);

        // Mutate the original. The view sees it.
        numbers.add(6);
        log("after numbers.add(6):");
        log("  numbers: " + numbers);
        log("  rev:     " + rev);

        // Mutate through the view. The original sees it.
        rev.removeFirst(); // removes 6 (which is rev's first because rev is reversed)
        log("after rev.removeFirst():");
        log("  numbers: " + numbers);
        log("  rev:     " + rev);

        // For an independent snapshot, copy explicitly.
        List<Integer> snapshot = List.copyOf(numbers.reversed());
        numbers.add(99);
        log("after numbers.add(99) (snapshot was taken before this):");
        log("  numbers:  " + numbers);
        log("  snapshot: " + snapshot + "   <- did not change");
    }

    /**
     * Demo 3: LinkedHashSet is a SequencedSet, so getFirst, getLast, addFirst,
     * addLast all work directly. addFirst of an element that already exists
     * moves it to the head; the set preserves the no-duplicates contract.
     */
    static void demoLinkedHashSet() {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("alpha");
        tags.add("beta");
        tags.add("gamma");
        tags.add("delta");
        log("tags (start): " + tags);

        log("getFirst: " + tags.getFirst());
        log("getLast:  " + tags.getLast());

        tags.addFirst("zeta");
        log("after addFirst(\"zeta\"): " + tags);

        // Add an existing element at the head: it moves, no duplicate.
        tags.addFirst("gamma");
        log("after addFirst(\"gamma\") (already present): " + tags);

        tags.addLast("alpha"); // already present; moves to the tail
        log("after addLast(\"alpha\")  (already present): " + tags);

        String head = tags.removeFirst();
        String tail = tags.removeLast();
        log("removeFirst returned: " + head);
        log("removeLast  returned: " + tail);
        log("tags (end):  " + tags);
    }

    /**
     * Demo 4: a method written against SequencedCollection<T> accepts a List,
     * a Deque, and a LinkedHashSet. The signature now expresses exactly what
     * the method needs (first-and-last access) without overcommitting to List.
     */
    static void demoWiderParameterType() {
        List<Integer> list = List.of(10, 20, 30, 40, 50);
        Deque<Integer> deque = new ArrayDeque<>(List.of(10, 20, 30, 40, 50));
        LinkedHashSet<Integer> linkedSet = new LinkedHashSet<>(List.of(10, 20, 30, 40, 50));

        log("range over List<Integer>:           " + firstAndLast(list));
        log("range over ArrayDeque<Integer>:     " + firstAndLast(deque));
        log("range over LinkedHashSet<Integer>:  " + firstAndLast(linkedSet));
    }

    /**
     * The point of the demo: this signature accepts every ordered collection
     * in the JDK. Before JEP 431, this method would have taken List<T> and
     * forced every other-shape caller to convert.
     */
    static <T> Range<T> firstAndLast(SequencedCollection<T> items) {
        return new Range<>(items.getFirst(), items.getLast());
    }

    // ---------- helpers and small types ----------

    static void log(String msg) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + msg);
    }

    static void section(String title) {
        System.out.println();
        System.out.println("==== " + title + " ====");
    }

    record Order(UUID id, String customer, int amount) {
        @Override
        public String toString() {
            return "Order{customer=" + customer + ", amount=" + amount + "}";
        }
    }

    record Range<T>(T first, T last) {
    }
}
