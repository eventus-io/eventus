package io.eventus.examples.payments.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final ApplicationEventPublisher events;

    public PaymentService(ApplicationEventPublisher events) {
        this.events = events;
    }

    @ApplicationModuleListener
    public void on(OrderPlaced order) {
        log.info("Processing payment for order {}, amount {}", order.orderId(), order.amount());

        // Simulate authorization — fail orders above 1000 to show PaymentFailed path
        if (order.amount().compareTo(new BigDecimal("1000")) > 0) {
            log.warn("Payment declined for order {} — amount exceeds limit", order.orderId());
            events.publishEvent(new PaymentFailed(order.orderId(), "AMOUNT_EXCEEDS_LIMIT"));
            return;
        }

        String authCode = "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Payment authorized for order {} with code {}", order.orderId(), authCode);
        events.publishEvent(new PaymentAuthorized(order.orderId(), order.customerId(), order.amount(), authCode));
    }

    @ApplicationModuleListener
    public void on(OrderCancelled order) {
        log.info("Processing refund for cancelled order {}", order.orderId());
        events.publishEvent(new PaymentRefunded(order.orderId(), BigDecimal.ZERO));
    }
}
