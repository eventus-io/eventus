package io.eventus.examples.modulith.analytics;

import io.eventus.examples.modulith.catalog.BookPublished;
import io.eventus.examples.modulith.catalog.PriceChanged;
import io.eventus.examples.modulith.fulfillment.ShipmentDelivered;
import io.eventus.examples.modulith.fulfillment.ShipmentDispatched;
import io.eventus.examples.modulith.orders.OrderCancelled;
import io.eventus.examples.modulith.orders.OrderConfirmed;
import io.eventus.examples.modulith.orders.OrderPlaced;
import io.eventus.examples.modulith.payments.PaymentAuthorized;
import io.eventus.examples.modulith.payments.PaymentFailed;
import io.eventus.examples.modulith.payments.PaymentRefunded;
import io.eventus.examples.modulith.reviews.ReviewPosted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-Ce analytics sink — subscribes to nearly every domain event.
 * Ce = 6 distinct publisher modules → very high instability (I ≈ 0.86).
 * Demonstrates the "god listener" anti-pattern in the topology view.
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final AtomicInteger ordersTotal     = new AtomicInteger();
    private final AtomicInteger ordersConfirmed = new AtomicInteger();
    private final AtomicInteger ordersCancelled = new AtomicInteger();
    private final AtomicInteger paymentsOk      = new AtomicInteger();
    private final AtomicInteger paymentsFailed  = new AtomicInteger();
    private final AtomicInteger refunds         = new AtomicInteger();
    private final AtomicInteger dispatched      = new AtomicInteger();
    private final AtomicInteger delivered       = new AtomicInteger();
    private final AtomicInteger reviews         = new AtomicInteger();
    private final AtomicInteger priceChanges    = new AtomicInteger();
    private final AtomicInteger booksPublished  = new AtomicInteger();

    @ApplicationModuleListener void onOrderPlaced(OrderPlaced e)           { log.debug("[ANALYTICS] order_placed={}", ordersTotal.incrementAndGet()); }
    @ApplicationModuleListener void onOrderConfirmed(OrderConfirmed e)     { log.debug("[ANALYTICS] order_confirmed={}", ordersConfirmed.incrementAndGet()); }
    @ApplicationModuleListener void onOrderCancelled(OrderCancelled e)     { log.debug("[ANALYTICS] order_cancelled={}", ordersCancelled.incrementAndGet()); }
    @ApplicationModuleListener void onPaymentAuthorized(PaymentAuthorized e){ log.debug("[ANALYTICS] payment_ok={}", paymentsOk.incrementAndGet()); }
    @ApplicationModuleListener void onPaymentFailed(PaymentFailed e)       { log.debug("[ANALYTICS] payment_failed={}", paymentsFailed.incrementAndGet()); }
    @ApplicationModuleListener void onPaymentRefunded(PaymentRefunded e)   { log.debug("[ANALYTICS] refund={}", refunds.incrementAndGet()); }
    @ApplicationModuleListener void onShipmentDispatched(ShipmentDispatched e){ log.debug("[ANALYTICS] dispatched={}", dispatched.incrementAndGet()); }
    @ApplicationModuleListener void onShipmentDelivered(ShipmentDelivered e)  { log.debug("[ANALYTICS] delivered={}", delivered.incrementAndGet()); }
    @ApplicationModuleListener void onReviewPosted(ReviewPosted e)         { log.debug("[ANALYTICS] review={}", reviews.incrementAndGet()); }
    @ApplicationModuleListener void onPriceChanged(PriceChanged e)         { log.debug("[ANALYTICS] price_change={}", priceChanges.incrementAndGet()); }
    @ApplicationModuleListener void onBookPublished(BookPublished e)       { log.debug("[ANALYTICS] book_published={}", booksPublished.incrementAndGet()); }

    public record Snapshot(int orders, int confirmed, int cancelled, int paymentsOk,
                           int paymentsFailed, int refunds, int dispatched, int delivered,
                           int reviews, int priceChanges, int booksPublished) {}

    public Snapshot snapshot() {
        return new Snapshot(ordersTotal.get(), ordersConfirmed.get(), ordersCancelled.get(),
                paymentsOk.get(), paymentsFailed.get(), refunds.get(),
                dispatched.get(), delivered.get(), reviews.get(),
                priceChanges.get(), booksPublished.get());
    }
}
