package io.eventus.examples.modulith.notifications;

import io.eventus.examples.modulith.catalog.BookPublished;
import io.eventus.examples.modulith.fulfillment.ShipmentDelivered;
import io.eventus.examples.modulith.fulfillment.ShipmentDispatched;
import io.eventus.examples.modulith.inventory.StockReleased;
import io.eventus.examples.modulith.inventory.StockReserved;
import io.eventus.examples.modulith.loyalty.LoyaltyPointsAwarded;
import io.eventus.examples.modulith.orders.OrderCancelled;
import io.eventus.examples.modulith.orders.OrderConfirmed;
import io.eventus.examples.modulith.orders.OrderPlaced;
import io.eventus.examples.modulith.payments.PaymentAuthorized;
import io.eventus.examples.modulith.payments.PaymentFailed;
import io.eventus.examples.modulith.payments.PaymentRefunded;
import io.eventus.examples.modulith.reviews.ReviewFlagged;
import io.eventus.examples.modulith.reviews.ReviewPosted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

/**
 * High-Ce notification sink — subscribes to events across 7 modules.
 * Demonstrates the "god listener" anti-pattern alongside analytics.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @ApplicationModuleListener
    void onOrderPlaced(OrderPlaced event) {
        log.info("[NOTIFY] Order received — customer={}, isbn={}, qty={}",
                event.customerId(), event.isbn(), event.quantity());
    }

    @ApplicationModuleListener
    void onOrderConfirmed(OrderConfirmed event) {
        log.info("[NOTIFY] Order confirmed — orderId={}, customer={}",
                event.orderId(), event.customerId());
    }

    @ApplicationModuleListener
    void onOrderCancelled(OrderCancelled event) {
        log.info("[NOTIFY] Order cancelled — orderId={}, reason={}",
                event.orderId(), event.reason());
    }

    @ApplicationModuleListener
    void onPaymentAuthorized(PaymentAuthorized event) {
        log.info("[NOTIFY] Payment authorised — orderId={}, amount=${}",
                event.orderId(), event.amount());
    }

    @ApplicationModuleListener
    void onPaymentFailed(PaymentFailed event) {
        log.warn("[NOTIFY] Payment failed — orderId={}, reason={}",
                event.orderId(), event.reason());
    }

    @ApplicationModuleListener
    void onPaymentRefunded(PaymentRefunded event) {
        log.info("[NOTIFY] Refund processed — orderId={}, amount=${}",
                event.orderId(), event.amount());
    }

    @ApplicationModuleListener
    void onStockReserved(StockReserved event) {
        log.info("[NOTIFY] Stock confirmed — orderId={}, isbn={}", event.orderId(), event.isbn());
    }

    @ApplicationModuleListener
    void onStockReleased(StockReleased event) {
        log.info("[NOTIFY] Stock returned to shelf — orderId={}, isbn={}", event.orderId(), event.isbn());
    }

    @ApplicationModuleListener
    void onShipmentDispatched(ShipmentDispatched event) {
        log.info("[NOTIFY] Your order is on its way! orderId={}, tracking={}",
                event.orderId(), event.trackingCode());
    }

    @ApplicationModuleListener
    void onShipmentDelivered(ShipmentDelivered event) {
        log.info("[NOTIFY] Order delivered — orderId={}, tracking={}",
                event.orderId(), event.trackingCode());
    }

    @ApplicationModuleListener
    void onReviewPosted(ReviewPosted event) {
        log.info("[NOTIFY] New review posted — isbn={}, rating={}", event.isbn(), event.rating());
    }

    @ApplicationModuleListener
    void onReviewFlagged(ReviewFlagged event) {
        log.warn("[NOTIFY] Review flagged — reviewId={}, reason={}", event.reviewId(), event.reason());
    }

    @ApplicationModuleListener
    void onLoyaltyPointsAwarded(LoyaltyPointsAwarded event) {
        log.info("[NOTIFY] You earned {} points! customer={}, reason={}",
                event.points(), event.customerId(), event.reason());
    }

    @ApplicationModuleListener
    void onBookPublished(BookPublished event) {
        log.info("[NOTIFY] New book available — isbn={}, title={}", event.isbn(), event.title());
    }
}
