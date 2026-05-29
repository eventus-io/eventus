package io.eventus.examples.modulith.payments;

public record PaymentAuthorized(String orderId, String customerId, double amount) {}
