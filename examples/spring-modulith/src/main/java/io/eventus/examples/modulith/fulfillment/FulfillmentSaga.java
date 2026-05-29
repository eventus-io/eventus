package io.eventus.examples.modulith.fulfillment;

import io.eventus.examples.modulith.inventory.StockReserved;
import io.eventus.examples.modulith.orders.OrderService;
import io.eventus.examples.modulith.payments.PaymentAuthorized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Choreography-based fulfillment saga.
 *
 * Waits for TWO independent events before dispatching:
 *   1. PaymentAuthorized  (from payments module)
 *   2. StockReserved      (from inventory module)
 *
 *   OrderPlaced ──▶ [payments]   ──▶ PaymentAuthorized ─┐
 *                                                        ├──▶ ShipmentDispatched
 *   OrderPlaced ──▶ [inventory]  ──▶ StockReserved    ──┘
 *
 * VIOLATION: also calls OrderService directly to confirm the order,
 * instead of publishing an event and letting orders react to it.
 */
@Component
public class FulfillmentSaga {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentSaga.class);

    private record SagaState(AtomicBoolean paymentDone, AtomicBoolean stockDone,
                              AtomicBoolean dispatched) {
        SagaState() { this(new AtomicBoolean(), new AtomicBoolean(), new AtomicBoolean()); }
    }

    private final ConcurrentHashMap<String, SagaState> states = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher events;
    // VIOLATION: direct service call into orders module
    private final OrderService orderService;

    public FulfillmentSaga(ApplicationEventPublisher events, OrderService orderService) {
        this.events = events;
        this.orderService = orderService;
    }

    @ApplicationModuleListener
    void onPaymentAuthorized(PaymentAuthorized event) {
        log.info("[SAGA] Payment authorised for order {}", event.orderId());
        state(event.orderId()).paymentDone().set(true);
        tryDispatch(event.orderId());
    }

    @ApplicationModuleListener
    void onStockReserved(StockReserved event) {
        log.info("[SAGA] Stock reserved for order {}", event.orderId());
        state(event.orderId()).stockDone().set(true);
        tryDispatch(event.orderId());
    }

    private void tryDispatch(String orderId) {
        SagaState s = states.get(orderId);
        if (s == null) return;
        if (s.paymentDone().get() && s.stockDone().get()
                && s.dispatched().compareAndSet(false, true)) {
            states.remove(orderId);
            String tracking = "TRK-" + orderId.substring(0, 8).toUpperCase();
            log.info("[SAGA] Both conditions met — dispatching {} ({})", orderId, tracking);
            events.publishEvent(new ShipmentDispatched(orderId, tracking));
            // Direct call: confirms the order without an event (intentional violation)
            orderService.confirm(orderId);
        }
    }

    private SagaState state(String id) {
        return states.computeIfAbsent(id, k -> new SagaState());
    }
}
