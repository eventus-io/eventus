package io.eventus.examples.modulith.orders;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orders, ApplicationEventPublisher events) {
        this.orders = orders;
        this.events = events;
    }

    public Order place(String isbn, int quantity, String customerId) {
        var order = new Order(UUID.randomUUID().toString(), isbn, customerId, quantity);
        orders.save(order);
        events.publishEvent(new OrderPlaced(order.getId(), isbn, quantity, customerId));
        return order;
    }

    public void cancel(String orderId, String reason) {
        orders.findById(orderId).ifPresent(order -> {
            order.cancel();
            orders.save(order);
            events.publishEvent(new OrderCancelled(orderId, order.getIsbn(), order.getQuantity(), order.getCustomerId(), reason));
        });
    }

    public void confirm(String orderId) {
        orders.findById(orderId).ifPresent(order -> {
            order.confirm();
            orders.save(order);
            events.publishEvent(new OrderConfirmed(orderId, order.getCustomerId()));
        });
    }

    public void ship(String orderId) {
        orders.findById(orderId).ifPresent(order -> {
            order.ship();
            orders.save(order);
        });
    }

    public void deliver(String orderId) {
        orders.findById(orderId).ifPresent(order -> {
            order.deliver();
            orders.save(order);
        });
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orders.findAll();
    }
}
