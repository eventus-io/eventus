package io.eventus.examples.payments.payment;

import java.math.BigDecimal;

// Local representation of the cross-service OrderPlaced event from the bookstore.
// Same simple name enables the Eventus UI to draw a cross-service edge.
public record OrderPlaced(String orderId, String customerId, BigDecimal amount) {}
