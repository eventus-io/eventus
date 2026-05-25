package io.eventus.examples.modulith.orders;

public record OrderPlaced(String orderId, String isbn, int quantity, String customerId) {}
