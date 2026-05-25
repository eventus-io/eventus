package io.eventus.spring.testapp.inventory;

import io.eventus.spring.testapp.order.OrderPlaced;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    @ApplicationModuleListener
    void on(OrderPlaced event) {
        // reserve inventory for the placed order
    }
}
