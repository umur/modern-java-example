package com.umur.modernjava.ch06;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class SealedExamples {

    // 1. A sealed interface with three record permits. The closed set is exactly
    //    these three alternatives. Each permit is a record (so it's implicitly
    //    final, satisfying the "every descendant must be sealed/non-sealed/final"
    //    rule for free).
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

    // 2. Switch expression with type patterns over the sealed interface. No
    //    'default'. The compiler verifies coverage by walking the permits list.
    //    Add a fourth permit and this method stops compiling at every site.
    public static String describe(OrderEvent event) {
        return switch (event) {
            case OrderPlaced p ->
                    "placed:    " + p.orderId() + " customer=" + p.customerId()
                            + " total=" + p.total();
            case OrderShipped s ->
                    "shipped:   " + s.orderId() + " via " + s.carrier()
                            + " (" + s.trackingNumber() + ")";
            case OrderCancelled c ->
                    "cancelled: " + c.orderId() + " reason=" + c.reason();
        };
    }

    // 3. A sealed class with a mix of permits. Two are records (final by
    //    construction). The third is non-sealed, which re-opens the hierarchy
    //    from that branch downward. Hexagon below extends Polygon at will.
    public abstract sealed static class Shape permits Circle, Square, Polygon {}

    public static final class Circle extends Shape {
        private final double radius;
        public Circle(double radius) { this.radius = radius; }
        public double radius() { return radius; }
    }

    public static final class Square extends Shape {
        private final double side;
        public Square(double side) { this.side = side; }
        public double side() { return side; }
    }

    // The non-sealed escape hatch. Polygon is part of the closed Shape set,
    // but anyone in the world can extend Polygon further. The exhaustiveness
    // check on Shape still has three branches; the open subhierarchy lives
    // inside the Polygon branch.
    public non-sealed static class Polygon extends Shape {
        private final int sides;
        public Polygon(int sides) { this.sides = sides; }
        public int sides() { return sides; }
    }

    public static class Hexagon extends Polygon {
        public Hexagon() { super(6); }
    }

    // 4. Exhaustive switch over the sealed Shape. Three branches, no default.
    //    Polygon catches every open extension (including Hexagon) without
    //    needing its own case.
    public static String area(Shape shape) {
        return switch (shape) {
            case Circle c  -> "circle area  = " + (Math.PI * c.radius() * c.radius());
            case Square s  -> "square area  = " + (s.side() * s.side());
            case Polygon p -> "polygon (" + p.sides() + " sides) area unknown without coords";
        };
    }

    public static void main(String[] args) {
        System.out.println("=== Modern Java in Practice, Chapter 6: Sealed Types ===");
        System.out.println();

        // 1. Build a small list of OrderEvents covering every permit.
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID customerId = UUID.fromString("00000000-0000-0000-0000-000000000010");

        List<OrderEvent> events = List.of(
                new OrderPlaced(orderId, customerId, new BigDecimal("42.50")),
                new OrderShipped(orderId, "DHL", "JD0123456789"),
                new OrderCancelled(orderId, "duplicate order")
        );

        System.out.println("--- Sealed interface + records + exhaustive switch ---");
        for (OrderEvent event : events) {
            System.out.println(describe(event));
        }
        System.out.println();

        // 2. Show that the type system actually rejects nonsense at compile time.
        //    We can't write this here because the compiler refuses, which is the
        //    whole point. Instead, print a note describing what the build would do.
        System.out.println("--- What you can't write ---");
        System.out.println("class StealthEvent implements OrderEvent {} -- compile error:");
        System.out.println("  'StealthEvent' is not allowed in the sealed hierarchy of 'OrderEvent'");
        System.out.println();

        // 3. Sealed class with a non-sealed branch.
        List<Shape> shapes = List.of(
                new Circle(2.0),
                new Square(3.0),
                new Polygon(5),
                new Hexagon()  // open subtype of Polygon, lives inside the Polygon branch
        );

        System.out.println("--- Sealed class with a non-sealed escape hatch ---");
        for (Shape shape : shapes) {
            System.out.println(area(shape));
        }
        System.out.println();

        // 4. Demonstrate the runtime types so the reader can see the open subtype
        //    falling through the Polygon branch.
        System.out.println("--- Runtime types ---");
        for (Shape shape : shapes) {
            System.out.println(shape.getClass().getSimpleName()
                    + " is-a Shape via branch: "
                    + (shape instanceof Circle ? "Circle"
                       : shape instanceof Square ? "Square"
                       : "Polygon"));
        }
    }
}
