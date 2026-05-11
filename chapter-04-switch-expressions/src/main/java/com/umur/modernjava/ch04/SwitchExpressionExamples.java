package com.umur.modernjava.ch04;

public class SwitchExpressionExamples {

    enum OrderStatus { PENDING, PAID, SHIPPED, CANCELLED, DELIVERED }

    enum DayOfWeek { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

    public static void main(String[] args) {
        System.out.println("=== Modern Java in Practice, Chapter 4: switch expressions ===");
        System.out.println();

        // Demo 1: legacy statement-form switch with colon labels and break.
        // The shape every Java codebase has thousands of.
        OrderStatus status = OrderStatus.PAID;
        String legacyAction;
        switch (status) {
            case PENDING:
                legacyAction = "wait";
                break;
            case PAID:
                legacyAction = "ship";
                break;
            case SHIPPED:
                legacyAction = "track";
                break;
            case CANCELLED:
                legacyAction = "refund";
                break;
            case DELIVERED:
                legacyAction = "close";
                break;
            default:
                legacyAction = "unknown";
        }
        System.out.println("Legacy statement form for " + status + ": " + legacyAction);

        // Demo 2: arrow-form switch expression.
        // Same logic, half the lines, no break, returns a value.
        String modernAction = switch (status) {
            case PENDING -> "wait";
            case PAID -> "ship";
            case SHIPPED -> "track";
            case CANCELLED -> "refund";
            case DELIVERED -> "close";
        };
        System.out.println("Arrow expression form for " + status + ": " + modernAction);

        // Demo 3: multi-label case.
        // Several enum values map to the same arm in one comma-separated list.
        DayOfWeek today = DayOfWeek.SATURDAY;
        String dayKind = switch (today) {
            case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "weekday";
            case SATURDAY, SUNDAY -> "weekend";
        };
        System.out.println("Multi-label case for " + today + ": " + dayKind);

        // Demo 4: block body with yield.
        // The right-hand side of -> can be a block. Inside that block, yield delivers
        // the case's value, just like return delivers a method's value.
        OrderStatus processing = OrderStatus.PAID;
        String detailedAction = switch (processing) {
            case PAID -> {
                System.out.println("  (block body) running fulfilment check for " + processing);
                boolean readyToShip = true;
                yield readyToShip ? "ship" : "backorder";
            }
            case PENDING -> "wait";
            case SHIPPED -> "track";
            case CANCELLED -> "refund";
            case DELIVERED -> "close";
        };
        System.out.println("Block body with yield for " + processing + ": " + detailedAction);

        // Demo 5: exhaustive switch over an enum, no default.
        // Listing every constant lets the compiler skip default. Adding a new
        // OrderStatus value later breaks this build, which is the safety net.
        for (OrderStatus s : OrderStatus.values()) {
            String label = switch (s) {
                case PENDING -> "awaiting payment";
                case PAID -> "ready for warehouse";
                case SHIPPED -> "in transit";
                case CANCELLED -> "to be refunded";
                case DELIVERED -> "complete";
            };
            System.out.println("Exhaustive (no default) for " + s + ": " + label);
        }
    }
}
