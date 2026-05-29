package io.eventus.examples.modulith.loyalty;

/** Intentionally has no consumers — demonstrates UNUSED_EVENT violation. */
public record LoyaltyPointsRedeemed(String customerId, int points, String orderId) {}
