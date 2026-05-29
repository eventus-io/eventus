package io.eventus.examples.modulith.inventory;

import io.eventus.examples.modulith.orders.OrderCancelled;
import io.eventus.examples.modulith.orders.OrderPlaced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventory;
    private final ApplicationEventPublisher events;

    public InventoryService(InventoryRepository inventory, ApplicationEventPublisher events) {
        this.inventory = inventory;
        this.events = events;
    }

    @ApplicationModuleListener
    void onOrderPlaced(OrderPlaced event) {
        inventory.findById(event.isbn()).ifPresentOrElse(
                item -> {
                    if (!item.hasAvailable(event.quantity())) {
                        log.warn("Insufficient stock for order {}: isbn={}, requested={}, available={}",
                                event.orderId(), event.isbn(), event.quantity(), item.getAvailableStock());
                        return;
                    }
                    item.reserve(event.quantity());
                    inventory.save(item);
                    events.publishEvent(new StockReserved(event.orderId(), event.isbn(), event.quantity()));
                    log.info("Stock reserved — order={}, isbn={}, qty={}", event.orderId(), event.isbn(), event.quantity());
                    if (item.getAvailableStock() == 0) {
                        events.publishEvent(new StockDepleted(event.isbn()));
                        log.warn("Stock depleted — isbn={}", event.isbn());
                    }
                },
                () -> log.warn("No inventory record for isbn={}", event.isbn())
        );
    }

    @ApplicationModuleListener
    void onOrderCancelled(OrderCancelled event) {
        inventory.findById(event.isbn()).ifPresent(item -> {
            item.release(event.quantity());
            inventory.save(item);
            events.publishEvent(new StockReleased(event.orderId(), event.isbn(), event.quantity()));
            log.info("Stock released — order={}, isbn={}, qty={}", event.orderId(), event.isbn(), event.quantity());
        });
    }

    public List<InventoryItem> findAll() {
        return inventory.findAll();
    }
}
