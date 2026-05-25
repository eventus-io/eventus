package io.eventus.examples.modulith.inventory;

public record StockReleased(String orderId, String isbn, int quantity) {}
