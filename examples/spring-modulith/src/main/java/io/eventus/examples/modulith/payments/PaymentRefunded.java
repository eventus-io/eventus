package io.eventus.examples.modulith.payments;

public record PaymentRefunded(String orderId, String customerId, double amount) {}
