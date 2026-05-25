package io.eventus.examples.modulith.orders;

public record PlaceOrderRequest(String isbn, int quantity, String customerId) {}
