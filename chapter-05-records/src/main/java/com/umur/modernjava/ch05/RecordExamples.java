package com.umur.modernjava.ch05;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecordExamples {

    // 1. Simplest possible record. Two components, everything generated.
    public record Customer(String name, String email) {}

    // 2. Record with a compact constructor that validates inputs.
    public record OrderTotal(BigDecimal amount, Currency currency) {
        public OrderTotal {
            if (amount == null) {
                throw new IllegalArgumentException("amount is required");
            }
            if (currency == null) {
                throw new IllegalArgumentException("currency is required");
            }
            if (amount.signum() < 0) {
                throw new IllegalArgumentException("amount cannot be negative");
            }
        }
    }

    // 3. Record implementing an interface, with helper methods and a static factory.
    public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

        public Money {
            if (amount == null) throw new IllegalArgumentException("amount required");
            if (currency == null) throw new IllegalArgumentException("currency required");
        }

        public static Money usd(String amount) {
            return new Money(new BigDecimal(amount), Currency.getInstance("USD"));
        }

        public Money plus(Money other) {
            if (!currency.equals(other.currency)) {
                throw new IllegalArgumentException("currency mismatch");
            }
            return new Money(amount.add(other.amount), currency);
        }

        @Override
        public int compareTo(Money other) {
            if (!currency.equals(other.currency)) {
                throw new IllegalArgumentException("currency mismatch");
            }
            return amount.compareTo(other.amount);
        }
    }

    // 4. A class that contains a nested static record as its return type.
    public static class CsvImporter {

        public record Result(int imported, int skipped, int errors) {}

        public Result importFile(String path) {
            // Pretend to parse. In real life this would read the file and tally.
            int imported = 12;
            int skipped = 3;
            int errors = 1;
            return new Result(imported, skipped, errors);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Modern Java in Practice, Chapter 5: Records ===");
        System.out.println();

        // 1. Simple record + auto-generated accessors and toString.
        var customer = new Customer("Alice", "alice@example.com");
        System.out.println("Customer name:  " + customer.name());
        System.out.println("Customer email: " + customer.email());
        System.out.println("Customer toString: " + customer);
        System.out.println();

        // 2. Compact constructor enforces invariants.
        var total = new OrderTotal(new BigDecimal("42.50"), Currency.getInstance("USD"));
        System.out.println("Valid OrderTotal: " + total);

        try {
            new OrderTotal(new BigDecimal("-1.00"), Currency.getInstance("USD"));
        } catch (IllegalArgumentException e) {
            System.out.println("Negative amount rejected: " + e.getMessage());
        }

        try {
            new OrderTotal(null, Currency.getInstance("USD"));
        } catch (IllegalArgumentException e) {
            System.out.println("Null amount rejected:     " + e.getMessage());
        }
        System.out.println();

        // 3. Record implementing an interface, with helper methods.
        var price       = Money.usd("19.99");
        var tax         = Money.usd("1.65");
        var grandTotal  = price.plus(tax);
        System.out.println("Money plus:        " + price + " + " + tax + " = " + grandTotal);
        System.out.println("Money compareTo:   " + price + " vs " + grandTotal + " => " + price.compareTo(grandTotal));
        System.out.println();

        // 4. Nested record returned from a method.
        var result = new CsvImporter().importFile("/tmp/orders.csv");
        System.out.println("Import result:    imported=" + result.imported()
                + " skipped=" + result.skipped()
                + " errors=" + result.errors());
        System.out.println();

        // 5. equals/hashCode/toString are auto-generated and structural.
        var alice1 = new Customer("Alice", "alice@example.com");
        var alice2 = new Customer("Alice", "alice@example.com");
        var bob    = new Customer("Bob",   "bob@example.com");

        System.out.println("alice1.equals(alice2): " + alice1.equals(alice2));   // true: same components
        System.out.println("alice1.equals(bob):    " + alice1.equals(bob));      // false
        System.out.println("alice1.hashCode == alice2.hashCode: "
                + (alice1.hashCode() == alice2.hashCode()));                      // true

        Set<Customer> uniques = new HashSet<>(List.of(alice1, alice2, bob));
        System.out.println("Set of {alice1, alice2, bob} size: " + uniques.size()); // 2
        System.out.println();

        // 6. Multi-return record declared right next to the method that uses it.
        var validation = validate(List.of("a@b.com", "not-an-email", "c@d.com", "also bad"));
        System.out.println("Validation: found=" + validation.found() + " errors=" + validation.errors());
    }

    // 6. Multi-return tuple. The record is local to this method's purpose.
    public record ValidationResult(int found, int errors) {}

    public static ValidationResult validate(List<String> emails) {
        int found = 0;
        int errors = 0;
        for (String email : emails) {
            if (email != null && email.contains("@") && !email.contains(" ")) {
                found++;
            } else {
                errors++;
            }
        }
        return new ValidationResult(found, errors);
    }
}
