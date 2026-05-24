# S05 — Actuator Endpoints

## Goal
Expose the graph data as three Spring Boot Actuator endpoints in `eventus-spring`.
These are the primary integration surface for v0.1 — no UI required yet.

## Acceptance Criteria
- [ ] Three endpoints registered and accessible via HTTP
- [ ] `/actuator/eventus/modules` returns list of modules as JSON
- [ ] `/actuator/eventus/events` returns list of events as JSON
- [ ] `/actuator/eventus/publications` returns list of incomplete/stale publications as JSON
- [ ] All endpoints return `200 OK` with `Content-Type: application/json`
- [ ] Endpoints return empty arrays `[]` when no data is available (never 404 or 500)
- [ ] Endpoint IDs are: `eventus-modules`, `eventus-events`, `eventus-publications`
- [ ] Endpoints are read-only (`@ReadOperation` only)
- [ ] Integration test verifies all three endpoints via `MockMvc` or `WebTestClient`

## JSON Response Shapes

### GET /actuator/eventus/modules
```json
[
  {
    "id": "order",
    "name": "Order",
    "beanCount": 4,
    "aggregateCount": 2,
    "status": "HEALTHY"
  }
]
```

### GET /actuator/eventus/events
```json
[
  {
    "id": "io.example.order.OrderPlaced",
    "name": "OrderPlaced",
    "publisherModuleId": "order"
  }
]
```

### GET /actuator/eventus/publications
```json
[
  {
    "id": "uuid-here",
    "eventType": "io.example.order.OrderPlaced",
    "listenerName": "io.example.inventory.InventoryModule.onOrderPlaced",
    "moduleId": "inventory",
    "status": "INCOMPLETE",
    "publishedAt": "2026-05-24T10:00:00Z"
  }
]
```

## Implementation

### EventusModulesEndpoint
```java
package io.eventus.spring.actuator;

import org.springframework.boot.actuate.endpoint.annotation.*;

@Endpoint(id = "eventus-modules")
public class EventusModulesEndpoint {

    private final GraphReader graphReader;

    public EventusModulesEndpoint(GraphReader graphReader) {
        this.graphReader = graphReader;
    }

    @ReadOperation
    public List<ModuleNode> modules() {
        return graphReader.getModules();
    }
}
```

### EventusEventsEndpoint
```java
@Endpoint(id = "eventus-events")
public class EventusEventsEndpoint {
    @ReadOperation
    public List<EventNode> events() { ... }
}
```

### EventusPublicationsEndpoint
```java
@Endpoint(id = "eventus-publications")
public class EventusPublicationsEndpoint {
    @ReadOperation
    public List<PublicationRecord> publications() {
        return graphReader.getIncompletePublications();
    }
}
```

## Package Structure
```
io.eventus.spring.actuator/
├── EventusModulesEndpoint.java
├── EventusEventsEndpoint.java
└── EventusPublicationsEndpoint.java
```

## Endpoint Exposure
Auto-configuration (S06) will add to `application.properties`:
```
management.endpoints.web.exposure.include=health,info,eventus-modules,eventus-events,eventus-publications
```

Or users can add it themselves. Document this in README.

## Tests Required

### EventusEndpointsIntegrationTest
Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with the test application
from S04 (`TestApplication`, `order` module, `inventory` module).

```java
@Test
void modulesEndpointReturnsModuleList() {
    // GET /actuator/eventus/modules
    // assert response contains "order" and "inventory"
}

@Test
void eventsEndpointReturnsEventList() {
    // GET /actuator/eventus/events
    // assert response contains event with name "OrderPlaced"
}

@Test
void publicationsEndpointReturnsEmptyWhenNoneIncomplete() {
    // GET /actuator/eventus/publications
    // assert response is []
}
```

## Done When
`mvn test -pl eventus-spring` passes including the endpoint integration tests.
All three endpoints return correct JSON when the test application is running.
