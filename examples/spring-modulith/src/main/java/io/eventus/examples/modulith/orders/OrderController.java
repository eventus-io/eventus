package io.eventus.examples.modulith.orders;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    List<Order> list() {
        return orderService.findAll();
    }

    @PostMapping
    ResponseEntity<Order> place(@RequestBody PlaceOrderRequest request) {
        return ResponseEntity.ok(orderService.place(request.isbn(), request.quantity(), request.customerId()));
    }

    @DeleteMapping("/{orderId}")
    ResponseEntity<Void> cancel(@PathVariable String orderId) {
        orderService.cancel(orderId, "customer-request");
        return ResponseEntity.noContent().build();
    }
}
