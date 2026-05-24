package io.eventus.spring.test.inventory;

import io.eventus.spring.test.order.OrderPlaced;
import org.springframework.modulith.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    @ApplicationModuleListener
    public void onOrderPlaced(OrderPlaced event) {
        // reserve inventory
    }
}
