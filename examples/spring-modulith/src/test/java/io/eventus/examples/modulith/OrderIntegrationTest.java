package io.eventus.examples.modulith;

import io.eventus.examples.modulith.inventory.InventoryItem;
import io.eventus.examples.modulith.inventory.InventoryRepository;
import io.eventus.examples.modulith.orders.OrderRepository;
import io.eventus.examples.modulith.orders.OrderService;
import io.eventus.examples.modulith.orders.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class OrderIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.save(new InventoryItem("isbn-test-1", 10));
        inventoryRepository.save(new InventoryItem("isbn-test-2", 5));
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        inventoryRepository.deleteById("isbn-test-1");
        inventoryRepository.deleteById("isbn-test-2");
    }

    @Test
    void placingOrderPublishesEventAndReservesStock() {
        var order = orderService.place("isbn-test-1", 3, "customer-001");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        // Listeners run async on a task executor — await their completion
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(inventoryRepository.findById("isbn-test-1"))
                        .hasValueSatisfying(item -> assertThat(item.getReservedStock()).isEqualTo(3)));
    }

    @Test
    void cancellingOrderReleasesReservedStock() {
        var order = orderService.place("isbn-test-2", 2, "customer-002");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(inventoryRepository.findById("isbn-test-2"))
                        .hasValueSatisfying(item -> assertThat(item.getReservedStock()).isEqualTo(2)));

        orderService.cancel(order.getId(), "changed-mind");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(inventoryRepository.findById("isbn-test-2"))
                        .hasValueSatisfying(item -> assertThat(item.getReservedStock()).isZero()));
    }
}
