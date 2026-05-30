package io.eventus.examples.payments.payment;

// Local representation of the cross-service OrderCancelled event from the bookstore.
public record OrderCancelled(String orderId, String reason) {}
