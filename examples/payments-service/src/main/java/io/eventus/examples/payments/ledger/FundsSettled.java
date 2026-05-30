package io.eventus.examples.payments.ledger;

public record FundsSettled(String orderId, String ledgerRef) {}
