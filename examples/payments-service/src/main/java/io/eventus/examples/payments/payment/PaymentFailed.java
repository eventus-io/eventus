package io.eventus.examples.payments.payment;

public record PaymentFailed(String orderId, String reason) {}
