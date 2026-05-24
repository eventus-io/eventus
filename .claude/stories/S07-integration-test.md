# S07 — End-to-End Integration Test

## Goal
Prove the full stack works together: auto-configuration wires the extractor,
extraction runs on startup, actuator endpoints return real data from a real
Spring Modulith application — all in a single test.

## Acceptance Criteria
- [ ] Integration test uses the test application from S04
      (`TestApplication`, `order` module, `inventory` module)
- [ ] Test starts the full Spring context (`@SpringBootTest`)
- [ ] `/actuator/eventus/modules` returns both "order" and "inventory"
- [ ] `/actuator/eventus/events` returns "OrderPlaced"
- [ ] The "OrderPlaced" event has `publisherModuleId` = "order"
- [ ] `/actuator/eventus/publications` returns `[]` (no incomplete publications in test)
- [ ] All assertions use JSON path expressions
- [ ] Test runs as part of `mvn verify` (not skipped)

## Test Application (reuse from S04, extend if needed)

```
src/test/java/io/eventus/spring/test/
├── TestApplication.java
├── EventusIntegrationTest.java
├── order/
│   ├── OrderService.java       (publishes OrderPlaced via ApplicationEventPublisher)
│   └── OrderPlaced.java        (record implementing DomainEvent)
└── inventory/
    └── InventoryService.java   (@ApplicationModuleListener for OrderPlaced)
```

### TestApplication.java
```java
@SpringBootApplication
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
```

### OrderPlaced.java
```java
package io.eventus.spring.test.order;

import org.springframework.modulith.events.DomainEvent; // or jMolecules

@DomainEvent
public record OrderPlaced(String orderId) {}
```

### OrderService.java
```java
package io.eventus.spring.test.order;

@Service
public class OrderService {
    private final ApplicationEventPublisher events;
    public OrderService(ApplicationEventPublisher events) { this.events = events; }
    public void placeOrder(String orderId) { events.publishEvent(new OrderPlaced(orderId)); }
}
```

### InventoryService.java
```java
package io.eventus.spring.test.inventory;

@Service
public class InventoryService {
    @ApplicationModuleListener
    public void onOrderPlaced(OrderPlaced event) {
        // reserve inventory
    }
}
```

## Integration Test

```java
package io.eventus.spring.test;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EventusIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void modulesEndpointReturnsBothModules() throws Exception {
        mockMvc.perform(get("/actuator/eventus-modules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", hasItems("order", "inventory")));
    }

    @Test
    void eventsEndpointReturnsOrderPlacedEvent() throws Exception {
        mockMvc.perform(get("/actuator/eventus-events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", hasItem("OrderPlaced")))
            .andExpect(jsonPath("$[?(@.name=='OrderPlaced')].publisherModuleId",
                hasItem("order")));
    }

    @Test
    void publicationsEndpointReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/actuator/eventus-publications"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
```

## Test application.properties

```properties
# src/test/resources/application.properties
management.endpoints.web.exposure.include=*
spring.modulith.events.jdbc-schema-initialization.enabled=false
```

## Done When
`mvn verify -pl eventus-spring` passes. The three integration test methods
are green. No manual steps required to run them.
