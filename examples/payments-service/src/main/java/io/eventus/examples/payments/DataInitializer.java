package io.eventus.examples.payments;

import io.eventus.examples.payments.payment.OrderCancelled;
import io.eventus.examples.payments.payment.OrderPlaced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ApplicationEventPublisher events;

    DataInitializer(ApplicationEventPublisher events) {
        this.events = events;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    void seed() {
        log.info("Seeding saga events...");

        // Happy path: authorized → risk cleared → settled
        events.publishEvent(new OrderPlaced("ORD-001", "CUST-A", new BigDecimal("49.99")));
        events.publishEvent(new OrderPlaced("ORD-002", "CUST-B", new BigDecimal("199.00")));
        events.publishEvent(new OrderPlaced("ORD-003", "CUST-C", new BigDecimal("89.50")));

        // Compensation path: cancelled → refunded
        events.publishEvent(new OrderCancelled("ORD-004", "CUSTOMER_REQUEST"));
    }
}
