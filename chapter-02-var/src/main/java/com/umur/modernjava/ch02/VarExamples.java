package com.umur.modernjava.ch02;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VarExamples {

    record Order(UUID id, String customer, int total) {}

    public static void main(String[] args) {
        System.out.println("=== Modern Java in Practice, Chapter 2: var ===");
        System.out.println();

        // Win 1: generic-soup type declaration
        var ordersByCustomer = sampleOrders();
        System.out.println("Generic-soup example: " + ordersByCustomer.size() + " customers");

        // Win 2: factory method return
        var recent = findRecentOrders();
        System.out.println("Factory return: " + recent.size() + " recent orders");

        // Win 3: for-each over complex generic
        for (var entry : ordersByCustomer.entrySet()) {
            System.out.println("  customer " + entry.getKey() + ": " + entry.getValue().size() + " orders");
        }

        // The diamond trap: ArrayList<Object>
        var list = new ArrayList<>();
        list.add("a String");
        list.add(42);                        // also fine because Object
        list.add(true);                      // also fine
        System.out.println("Diamond trap: " + list);

        // Replace with explicit generics for the right type:
        var safeList = new ArrayList<String>();
        safeList.add("only Strings allowed here");
        System.out.println("Explicit generics: " + safeList);
    }

    static Map<String, List<Order>> sampleOrders() {
        var byCustomer = new HashMap<String, List<Order>>();
        byCustomer.put("alice", List.of(new Order(UUID.randomUUID(), "alice", 100)));
        byCustomer.put("bob", List.of(
            new Order(UUID.randomUUID(), "bob", 50),
            new Order(UUID.randomUUID(), "bob", 75)));
        return byCustomer;
    }

    static List<Order> findRecentOrders() {
        return List.of(
            new Order(UUID.randomUUID(), "alice", 100),
            new Order(UUID.randomUUID(), "carol", 200));
    }
}
