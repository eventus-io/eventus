package io.eventus.examples.payments.payment;

import java.math.BigDecimal;

public record PaymentRefunded(String orderId, BigDecimal amount) {}
