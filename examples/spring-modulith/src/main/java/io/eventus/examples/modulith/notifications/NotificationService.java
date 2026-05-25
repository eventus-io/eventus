package io.eventus.examples.modulith.notifications;

import io.eventus.examples.modulith.inventory.StockReleased;
import io.eventus.examples.modulith.inventory.StockReserved;
import io.eventus.examples.modulith.orders.OrderCancelled;
import io.eventus.examples.modulith.orders.OrderPlaced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @ApplicationModuleListener
    void onOrderPlaced(OrderPlaced event) {
        log.info("[NOTIFY] Order received — customer={}, isbn={}, qty={}",
                event.customerId(), event.isbn(), event.quantity());
    }

    @ApplicationModuleListener
    void onOrderCancelled(OrderCancelled event) {
        log.info("[NOTIFY] Order cancelled — orderId={}, reason={}",
                event.orderId(), event.reason());
    }

    @ApplicationModuleListener
    void onStockReserved(StockReserved event) {
        log.info("[NOTIFY] Stock confirmed — orderId={}, isbn={}", event.orderId(), event.isbn());
    }

    @ApplicationModuleListener
    void onStockReleased(StockReleased event) {
        log.info("[NOTIFY] Stock returned to shelf — orderId={}, isbn={}", event.orderId(), event.isbn());
    }
}
