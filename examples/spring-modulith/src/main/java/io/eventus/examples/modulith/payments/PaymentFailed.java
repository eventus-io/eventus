package io.eventus.examples.modulith.payments;

public record PaymentFailed(String orderId, String customerId, String reason) {}
