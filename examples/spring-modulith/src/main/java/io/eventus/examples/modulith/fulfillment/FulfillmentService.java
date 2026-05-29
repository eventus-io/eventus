package io.eventus.examples.modulith.fulfillment;

import io.eventus.examples.modulith.orders.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** Handles manual delivery confirmation for demo purposes. */
@Service
public class FulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentService.class);

    private final ApplicationEventPublisher events;
    private final OrderService orderService;

    public FulfillmentService(ApplicationEventPublisher events, OrderService orderService) {
        this.events = events;
        this.orderService = orderService;
    }

    public void markDelivered(String orderId, String trackingCode) {
        log.info("[FULFILLMENT] Delivered {} ({})", orderId, trackingCode);
        events.publishEvent(new ShipmentDelivered(orderId, trackingCode));
    }
}
