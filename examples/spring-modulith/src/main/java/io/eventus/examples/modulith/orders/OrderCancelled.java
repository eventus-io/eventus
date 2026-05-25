package io.eventus.examples.modulith.orders;

public record OrderCancelled(String orderId, String isbn, int quantity, String reason) {}
