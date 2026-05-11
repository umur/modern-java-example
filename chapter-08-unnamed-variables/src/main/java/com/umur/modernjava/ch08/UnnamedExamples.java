package com.umur.modernjava.ch08;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UnnamedExamples {

    // ---------- Sealed OrderEvent hierarchy (chapter 6 + 7 carry-over) ----------

    public sealed interface OrderEvent
            permits OrderPlaced, OrderShipped, OrderCancelled {}

    public record OrderPlaced(
            UUID orderId,
            UUID customerId,
            BigDecimal total) implements OrderEvent {}

    public record OrderShipped(
            UUID orderId,
            String carrier,
            String trackingNumber) implements OrderEvent {}

    public record OrderCancelled(
            UUID orderId,
            String reason) implements OrderEvent {}

    // ---------- Nested records for the pattern examples ----------

    public record Customer(String name, String email) {}
    public record Address(String street, String city) {}
    public record Order(Customer customer, Address shipTo, BigDecimal total) {}

    // ---------- A tiny fake "transaction" so try-with-resources has something to close ----------

    static final class Transaction implements AutoCloseable {
        private final String label;
        Transaction(String label) {
            this.label = label;
            System.out.println("    [tx open]  " + label);
        }
        @Override public void close() {
            System.out.println("    [tx close] " + label);
        }
    }

    static Transaction openTransaction(String label) {
        return new Transaction(label);
    }

    static boolean tryAcquireLock(String key) {
        // Pretend this acquires a distributed lock. Caller doesn't read the result.
        return true;
    }

    // ---------- 8.3: unnamed variables ----------

    // Try-with-resources where the resource is opened for the side effect only.
    static void saveOrderInTransaction(String orderId) {
        try (var _ = openTransaction("save " + orderId)) {
            // The body never references the transaction. The framework commits/rolls back at scope exit.
            System.out.println("    saving order " + orderId);
        }
    }

    // Exception handler where the exception object is never used.
    static int parseOrZero(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException _) {
            return 0;
        }
    }

    // Local from a side-effecting call. The caller wants the side effect, not the return.
    static void warmCache() {
        var _ = tryAcquireLock("warmup-job");
        System.out.println("    cache warmed");
    }

    // Lambda where one parameter is unused. Map.forEach is the canonical case.
    static void logKeysOnly(Map<UUID, OrderEvent> events) {
        events.forEach((eventId, _) -> System.out.println("    saw event id " + eventId));
    }

    // For-each loop where the value isn't used. Counting only.
    static int countItems(List<OrderEvent> items) {
        int count = 0;
        for (var _ : items) {
            count++;
        }
        return count;
    }

    // ---------- 8.4: unnamed patterns ----------

    // Components the arm doesn't need become _.
    static String describeIdsOnly(OrderEvent event) {
        return switch (event) {
            case OrderPlaced(var orderId, _, _)   -> "placed    " + orderId;
            case OrderShipped(var orderId, _, _)  -> "shipped   " + orderId;
            case OrderCancelled(var orderId, _)   -> "cancelled " + orderId;
        };
    }

    // Pattern with a guard. The guard refers to bound names; ignored components stay _.
    static String classifyShipment(OrderEvent event) {
        return switch (event) {
            case OrderShipped(_, var carrier, _) when "DHL".equals(carrier) ->
                    "DHL shipment routed";
            case OrderShipped(_, var carrier, _) ->
                    "shipment via " + carrier;
            case OrderPlaced _    -> "not yet shipped (placed)";
            case OrderCancelled _ -> "cancelled, no shipment";
        };
    }

    // Nested patterns: walk the tree, name the leaves we want, ignore the rest.
    static String greeting(Order order) {
        return switch (order) {
            case Order(Customer(var name, _), _, _) -> "Hello, " + name;
        };
    }

    // Nested with a deeper destructure: city only.
    static String shipToCity(Order order) {
        return switch (order) {
            case Order(_, Address(_, var city), _) -> "ships to " + city;
        };
    }

    // ---------- main ----------

    public static void main(String[] args) {
        System.out.println("=== Modern Java in Practice, Chapter 8: Unnamed Variables and Patterns ===");
        System.out.println();

        UUID orderId    = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID customerId = UUID.fromString("00000000-0000-0000-0000-000000000010");

        // 8.3: unnamed variables
        System.out.println("--- 8.3: try-with-resources, resource is _ ---");
        saveOrderInTransaction("ord-001");
        System.out.println();

        System.out.println("--- 8.3: catch with unnamed exception ---");
        System.out.println("parseOrZero(\"42\")    = " + parseOrZero("42"));
        System.out.println("parseOrZero(\"oops\")  = " + parseOrZero("oops"));
        System.out.println();

        System.out.println("--- 8.3: local from side-effecting call ---");
        warmCache();
        System.out.println();

        System.out.println("--- 8.3: lambda with unnamed second parameter ---");
        Map<UUID, OrderEvent> eventsById = new LinkedHashMap<>();
        eventsById.put(orderId,
                new OrderPlaced(orderId, customerId, new BigDecimal("42.50")));
        eventsById.put(UUID.fromString("00000000-0000-0000-0000-000000000002"),
                new OrderShipped(orderId, "DHL", "JD0123456789"));
        logKeysOnly(eventsById);
        System.out.println();

        System.out.println("--- 8.3: for-each with unnamed loop variable ---");
        List<OrderEvent> events = List.of(
                new OrderPlaced(orderId, customerId, new BigDecimal("42.50")),
                new OrderShipped(orderId, "DHL", "JD0123456789"),
                new OrderCancelled(orderId, "duplicate order")
        );
        System.out.println("countItems(events) = " + countItems(events));
        System.out.println();

        // 8.4: unnamed patterns
        System.out.println("--- 8.4: record patterns with _ for ignored components ---");
        for (OrderEvent event : events) {
            System.out.println(describeIdsOnly(event));
        }
        System.out.println();

        System.out.println("--- 8.4: pattern with guard, ignored components stay _ ---");
        for (OrderEvent event : events) {
            System.out.println(classifyShipment(event));
        }
        System.out.println();

        System.out.println("--- 8.4: nested record patterns mixing named and unnamed ---");
        Order order = new Order(
                new Customer("Ada Lovelace", "ada@example.com"),
                new Address("21 Analytical Engine Rd", "London"),
                new BigDecimal("999.00")
        );
        System.out.println(greeting(order));
        System.out.println(shipToCity(order));
        System.out.println();

        System.out.println("=== done ===");
    }
}
