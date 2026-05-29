package io.eventus.examples.modulith.reviews;

public record ReviewPosted(String reviewId, String isbn, String customerId, int rating) {}
