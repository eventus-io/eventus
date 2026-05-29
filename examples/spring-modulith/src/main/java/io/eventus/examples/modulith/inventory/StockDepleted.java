package io.eventus.examples.modulith.inventory;

/** Published when stock hits zero. Intentionally has no consumers — demonstrates UNUSED_EVENT violation. */
public record StockDepleted(String isbn) {}
