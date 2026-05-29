package io.eventus.examples.modulith.payments;

import io.eventus.examples.modulith.orders.OrderPlaced;
import io.eventus.examples.modulith.orders.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final ApplicationEventPublisher events;
    // VIOLATION: direct service call into orders — payment cancels the order
    // instead of publishing PaymentFailed and letting orders react.
    private final OrderService orderService;

    public PaymentService(PaymentRepository payments, ApplicationEventPublisher events,
                          OrderService orderService) {
        this.payments = payments;
        this.events = events;
        this.orderService = orderService;
    }

    @ApplicationModuleListener
    void onOrderPlaced(OrderPlaced event) {
        double amount = event.quantity() * 24.99;
        var payment = new Payment(UUID.randomUUID().toString(),
                event.orderId(), event.customerId(), amount);

        // Quantities divisible by 7 fail — surfaces payment failure path in the demo
        if (event.quantity() % 7 == 0) {
            payment.fail();
            payments.save(payment);
            log.warn("[PAYMENT] Declined order {} — ${}", event.orderId(), amount);
            events.publishEvent(new PaymentFailed(event.orderId(), event.customerId(),
                    "Card declined for $" + amount));
            // Direct call: bypasses event bus (intentional coupling violation)
            orderService.cancel(event.orderId(), "Payment declined: $" + amount);
        } else {
            payment.authorize();
            payments.save(payment);
            log.info("[PAYMENT] Authorized ${} for order {}", amount, event.orderId());
            events.publishEvent(new PaymentAuthorized(event.orderId(), event.customerId(), amount));
        }
    }

    public void refund(String orderId) {
        payments.findByOrderId(orderId).ifPresent(p -> {
            p.refund();
            payments.save(p);
            events.publishEvent(new PaymentRefunded(orderId, p.getCustomerId(), p.getAmount()));
            log.info("[PAYMENT] Refunded ${} for order {}", p.getAmount(), orderId);
        });
    }
}
