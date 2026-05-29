package io.eventus.examples.modulith.catalog;

public record PriceChanged(String isbn, double oldPrice, double newPrice) {}
