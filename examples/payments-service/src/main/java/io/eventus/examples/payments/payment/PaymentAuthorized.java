package io.eventus.examples.payments.payment;

import java.math.BigDecimal;

public record PaymentAuthorized(String orderId, String customerId, BigDecimal amount, String authCode) {}
