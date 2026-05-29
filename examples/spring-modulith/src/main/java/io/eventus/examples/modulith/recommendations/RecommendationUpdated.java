package io.eventus.examples.modulith.recommendations;

/** Intentionally has no consumers — demonstrates UNUSED_EVENT violation. */
public record RecommendationUpdated(String isbn, double score) {}
