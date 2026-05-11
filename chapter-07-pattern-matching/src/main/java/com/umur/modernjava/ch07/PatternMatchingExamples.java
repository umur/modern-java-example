package com.umur.modernjava.ch07;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class PatternMatchingExamples {

    // ---------- Sealed OrderEvent hierarchy (chapter 6 carry-over) ----------

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

    // ---------- 7.2: instanceof patterns ----------

    // Old form: test, cast, bind, on three lines.
    public static String describeOld(Object obj) {
        if (obj instanceof OrderShipped) {
            OrderShipped shipped = (OrderShipped) obj;
            return "old: shipped " + shipped.orderId() + " via " + shipped.carrier();
        }
        return "old: not a shipment";
    }

    // New form: test, bind, in one expression. The cast is gone.
    public static String describeNew(Object obj) {
        if (obj instanceof OrderShipped shipped) {
            return "new: shipped " + shipped.orderId() + " via " + shipped.carrier();
        }
        return "new: not a shipment";
    }

    // The early-return idiom. The binding survives outside the if's block
    // because the compiler can prove the only path past the guard is the
    // path where the type test succeeded.
    public static String earlyReturn(Object obj) {
        if (!(obj instanceof OrderShipped shipped)) {
            return "early-return: not a shipment, ignored";
        }
        // 'shipped' is in scope here, the rest of the method, with type OrderShipped.
        String carrier = shipped.carrier();
        String tracking = shipped.trackingNumber();
        return "early-return: routed " + carrier + " tracking " + tracking;
    }

    // ---------- 7.3: switch patterns over a sealed hierarchy ----------

    // Type patterns. Three branches, no default, exhaustiveness proven by the
    // compiler walking the permits clause.
    public static String describeWithTypePattern(OrderEvent event) {
        return switch (event) {
            case OrderPlaced p ->
                    "type: placed    " + p.orderId() + " customer=" + p.customerId();
            case OrderShipped s ->
                    "type: shipped   " + s.orderId() + " via " + s.carrier();
            case OrderCancelled c ->
                    "type: cancelled " + c.orderId() + " reason=" + c.reason();
        };
    }

    // ---------- 7.4: record patterns ----------

    // Destructure each record into its components in the same step as the
    // type test. var infers component types from the record declaration.
    public static String describeWithRecordPattern(OrderEvent event) {
        return switch (event) {
            case OrderPlaced(var orderId, var customerId, var total) ->
                    "record: placed " + orderId + " for " + customerId + " total " + total;
            case OrderShipped(var orderId, var carrier, var trackingNumber) ->
                    "record: shipped " + orderId + " via " + carrier + " (" + trackingNumber + ")";
            case OrderCancelled(var orderId, var reason) ->
                    "record: cancelled " + orderId + ": " + reason;
        };
    }

    // ---------- Nested record patterns ----------

    public record Customer(String name, String email) {}
    public record Address(String street, String city) {}
    public record Order(Customer customer, Address shipTo, BigDecimal total) {}

    // One case arm. Six bindings, walked from the leaves out.
    public static String summary(Order order) {
        return switch (order) {
            case Order(Customer(var name, var email), Address(var street, var city), var total) ->
                    "nested: " + name + " <" + email + "> at " + street + ", " + city
                            + " total=" + total;
        };
    }

    // ---------- 7.5: guard clauses ----------

    public record Discount(int percent) {}

    // case Pattern when condition. The first case fires for percent > 50; the
    // rest fall through to the unguarded case below, which carries the
    // exhaustiveness guarantee.
    public static String classify(Discount discount) {
        return switch (discount) {
            case Discount d when d.percent() > 50 -> "guard: premium  (" + d.percent() + "%)";
            case Discount d when d.percent() > 0  -> "guard: standard (" + d.percent() + "%)";
            case Discount d                       -> "guard: none     (" + d.percent() + "%)";
        };
    }

    // ---------- 7.6: replacing the Visitor pattern ----------

    // A small AST. Three node types, all records, sealed parent. Permits are
    // inferred from the file because the records live inside Expr.
    public sealed interface Expr {
        record IntLiteral(int value)            implements Expr {}
        record Add(Expr left, Expr right)       implements Expr {}
        record Multiply(Expr left, Expr right)  implements Expr {}
    }

    // The whole evaluator is one switch with three arms. No accept method.
    // No Visitor interface. Recursive descent uses direct method calls.
    public static int evaluate(Expr expr) {
        return switch (expr) {
            case Expr.IntLiteral(int value)         -> value;
            case Expr.Add(var left, var right)      -> evaluate(left) + evaluate(right);
            case Expr.Multiply(var left, var right) -> evaluate(left) * evaluate(right);
        };
    }

    // Adding a new operation is a new method, not a new visitor class.
    public static String render(Expr expr) {
        return switch (expr) {
            case Expr.IntLiteral(int value)         -> String.valueOf(value);
            case Expr.Add(var left, var right)      -> "(" + render(left) + " + " + render(right) + ")";
            case Expr.Multiply(var left, var right) -> "(" + render(left) + " * " + render(right) + ")";
        };
    }

    // ---------- main ----------

    public static void main(String[] args) {
        System.out.println("=== Modern Java in Practice, Chapter 7: Pattern Matching ===");
        System.out.println();

        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID customerId = UUID.fromString("00000000-0000-0000-0000-000000000010");

        OrderShipped sample = new OrderShipped(orderId, "DHL", "JD0123456789");
        OrderPlaced placed = new OrderPlaced(orderId, customerId, new BigDecimal("42.50"));

        // 7.2: instanceof patterns
        System.out.println("--- 7.2: instanceof patterns ---");
        System.out.println(describeOld(sample));
        System.out.println(describeNew(sample));
        System.out.println(describeNew("not an event"));
        System.out.println(earlyReturn(sample));
        System.out.println(earlyReturn(placed));
        System.out.println();

        // 7.3 + 7.4: switch patterns and record patterns
        List<OrderEvent> events = List.of(
                new OrderPlaced(orderId, customerId, new BigDecimal("42.50")),
                new OrderShipped(orderId, "DHL", "JD0123456789"),
                new OrderCancelled(orderId, "duplicate order")
        );

        System.out.println("--- 7.3: switch patterns (type patterns) ---");
        for (OrderEvent event : events) {
            System.out.println(describeWithTypePattern(event));
        }
        System.out.println();

        System.out.println("--- 7.4: record patterns ---");
        for (OrderEvent event : events) {
            System.out.println(describeWithRecordPattern(event));
        }
        System.out.println();

        System.out.println("--- 7.4: nested record patterns ---");
        Order order = new Order(
                new Customer("Ada Lovelace", "ada@example.com"),
                new Address("21 Analytical Engine Rd", "London"),
                new BigDecimal("999.00")
        );
        System.out.println(summary(order));
        System.out.println();

        // 7.5: guards
        System.out.println("--- 7.5: guard clauses ---");
        List<Discount> discounts = List.of(
                new Discount(75),
                new Discount(20),
                new Discount(0)
        );
        for (Discount d : discounts) {
            System.out.println(classify(d));
        }
        System.out.println();

        // 7.6: visitor refactor
        System.out.println("--- 7.6: replacing the visitor pattern ---");
        // (3 + 4) * 5
        Expr ast = new Expr.Multiply(
                new Expr.Add(new Expr.IntLiteral(3), new Expr.IntLiteral(4)),
                new Expr.IntLiteral(5)
        );
        System.out.println("expression: " + render(ast));
        System.out.println("evaluated:  " + evaluate(ast));
        System.out.println();

        System.out.println("=== done ===");
    }
}
