package io.eventus.examples.modulith.loyalty;

import io.eventus.examples.modulith.fulfillment.ShipmentDispatched;
import io.eventus.examples.modulith.orders.OrderCancelled;
import io.eventus.examples.modulith.orders.OrderConfirmed;
import io.eventus.examples.modulith.orders.OrderPlaced;
import io.eventus.examples.modulith.payments.PaymentRefunded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class LoyaltyService {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyService.class);

    private final LoyaltyRepository accounts;
    private final ApplicationEventPublisher events;

    public LoyaltyService(LoyaltyRepository accounts, ApplicationEventPublisher events) {
        this.accounts = accounts;
        this.events = events;
    }

    @ApplicationModuleListener
    void onOrderPlaced(OrderPlaced e) { ensureAccount(e.customerId()); }

    @ApplicationModuleListener
    void onOrderConfirmed(OrderConfirmed e) { award(e.customerId(), 10, "order confirmed"); }

    @ApplicationModuleListener
    void onOrderCancelled(OrderCancelled e) {
        accounts.findById(e.customerId()).ifPresent(a -> {
            a.redeem(5);
            accounts.save(a);
            log.info("[LOYALTY] Deducted 5pts from {} for cancellation", e.customerId());
        });
    }

    @ApplicationModuleListener
    void onShipmentDispatched(ShipmentDispatched e) {
        log.debug("[LOYALTY] Shipment dispatched for order {} — bonus TBD", e.orderId());
    }

    @ApplicationModuleListener
    void onPaymentRefunded(PaymentRefunded e) { award(e.customerId(), 5, "refund goodwill"); }

    public void redeem(String customerId, int points, String orderId) {
        accounts.findById(customerId).ifPresent(a -> {
            a.redeem(points);
            accounts.save(a);
            events.publishEvent(new LoyaltyPointsRedeemed(customerId, points, orderId));
            log.info("[LOYALTY] {} redeemed {} pts for order {}", customerId, points, orderId);
        });
    }

    private void award(String customerId, int pts, String reason) {
        LoyaltyAccount a = accounts.findById(customerId)
                .orElseGet(() -> accounts.save(new LoyaltyAccount(customerId)));
        a.award(pts);
        accounts.save(a);
        events.publishEvent(new LoyaltyPointsAwarded(customerId, pts, reason));
        log.info("[LOYALTY] Awarded {}pts to {} — {}", pts, customerId, reason);
    }

    private void ensureAccount(String customerId) {
        if (!accounts.existsById(customerId)) accounts.save(new LoyaltyAccount(customerId));
    }
}
