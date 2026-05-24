# S11 — Impact Analysis API

## Goal
Expose graph traversal queries through REST endpoints so users can answer "what breaks if I change this event?"

## Acceptance Criteria
- [ ] `/eventus/api/impact/event/{eventId}` returns all modules that will be affected
- [ ] `/eventus/api/impact/module/{moduleId}` returns all downstream modules
- [ ] Response includes count of direct + indirect dependents
- [ ] Response includes all listener module names
- [ ] Response shows event names that would be affected
- [ ] Proper error handling: 404 if eventId/moduleId not found
- [ ] All queries complete in <200ms for graphs up to 100 modules

## API Specification

### Get Event Impact

```
GET /eventus/api/impact/event/{eventId}
```

**Response:**

```json
{
  "eventId": "com.example.order.OrderPlaced",
  "eventName": "OrderPlaced",
  "publisherModuleId": "order",
  "directListeners": 2,
  "indirectConsumers": 1,
  "affectedModules": [
    {
      "moduleId": "inventory",
      "moduleName": "Inventory",
      "relationshipType": "LISTENS_TO",
      "isDirectListener": true
    },
    {
      "moduleId": "notification",
      "moduleName": "Notification",
      "relationshipType": "LISTENS_TO",
      "isDirectListener": true
    },
    {
      "moduleId": "analytics",
      "moduleName": "Analytics",
      "relationshipType": "DEPENDS_ON",
      "isDirectListener": false
    }
  ]
}
```

### Get Module Impact

```
GET /eventus/api/impact/module/{moduleId}
```

**Response:**

```json
{
  "moduleId": "order",
  "moduleName": "Order",
  "publishedEvents": [
    {
      "eventId": "com.example.order.OrderPlaced",
      "eventName": "OrderPlaced",
      "directListeners": 2,
      "listenersModuleIds": ["inventory", "notification"]
    }
  ],
  "downstreamModules": [
    {
      "moduleId": "inventory",
      "moduleName": "Inventory",
      "relationshipType": "EVENT_LISTENER"
    }
  ],
  "totalAffectedModules": 3
}
```

## Implementation

### Value Objects

```java
// io.eventus.core.impact/
public record EventImpactResponse(
    String eventId,
    String eventName,
    String publisherModuleId,
    int directListeners,
    int indirectConsumers,
    List<AffectedModule> affectedModules
) {}

public record AffectedModule(
    String moduleId,
    String moduleName,
    String relationshipType,  // LISTENS_TO, DEPENDS_ON
    boolean isDirectListener
) {}

public record ModuleImpactResponse(
    String moduleId,
    String moduleName,
    List<EventInfo> publishedEvents,
    List<DownstreamModule> downstreamModules,
    int totalAffectedModules
) {}

public record EventInfo(
    String eventId,
    String eventName,
    int directListeners,
    List<String> listenerModuleIds
) {}

public record DownstreamModule(
    String moduleId,
    String moduleName,
    String relationshipType
) {}
```

### Graph Traversal Service (in eventus-core)

```java
// io.eventus.core.impact/ImpactAnalyzer.java
public interface ImpactAnalyzer {
    EventImpactResponse analyzeEventImpact(String eventId);
    ModuleImpactResponse analyzeModuleImpact(String moduleId);
}

// io.eventus.core.impact/InMemoryImpactAnalyzer.java
public class InMemoryImpactAnalyzer implements ImpactAnalyzer {
    private final GraphReader graphReader;

    public InMemoryImpactAnalyzer(GraphReader graphReader) {
        this.graphReader = graphReader;
    }

    @Override
    public EventImpactResponse analyzeEventImpact(String eventId) {
        EventNode event = graphReader.getEvents().stream()
            .filter(e -> e.id().equals(eventId))
            .findFirst()
            .orElseThrow(() -> new EventNotFoundException(eventId));

        List<EventEdge> edges = graphReader.getEdgesForEvent(eventId);
        
        List<AffectedModule> affected = edges.stream()
            .filter(e -> e.edgeType() == EdgeType.LISTENS_TO)
            .map(e -> new AffectedModule(
                e.toModuleId(),
                graphReader.getModules().stream()
                    .filter(m -> m.id().equals(e.toModuleId()))
                    .map(ModuleNode::name)
                    .findFirst()
                    .orElse(e.toModuleId()),
                e.edgeType().name(),
                true
            ))
            .distinct()
            .toList();

        return new EventImpactResponse(
            event.id(),
            event.name(),
            event.publisherModuleId(),
            affected.size(),
            calculateIndirectConsumers(affected),
            affected
        );
    }

    @Override
    public ModuleImpactResponse analyzeModuleImpact(String moduleId) {
        if (!graphReader.getModules().stream().anyMatch(m -> m.id().equals(moduleId))) {
            throw new ModuleNotFoundException(moduleId);
        }

        ModuleNode module = graphReader.getModules().stream()
            .filter(m -> m.id().equals(moduleId))
            .findFirst()
            .get();

        List<EventNode> published = graphReader.getEvents().stream()
            .filter(e -> e.publisherModuleId().equals(moduleId))
            .toList();

        List<EventInfo> eventInfos = published.stream()
            .map(event -> {
                List<EventEdge> edges = graphReader.getEdgesForEvent(event.id());
                List<String> listeners = edges.stream()
                    .filter(e -> e.edgeType() == EdgeType.LISTENS_TO)
                    .map(EventEdge::toModuleId)
                    .distinct()
                    .toList();
                return new EventInfo(event.id(), event.name(), listeners.size(), listeners);
            })
            .toList();

        Set<String> downstream = new HashSet<>();
        eventInfos.forEach(ei -> downstream.addAll(ei.listenerModuleIds()));

        return new ModuleImpactResponse(
            module.id(),
            module.name(),
            eventInfos,
            downstream.stream()
                .map(id -> new DownstreamModule(
                    id,
                    graphReader.getModules().stream()
                        .filter(m -> m.id().equals(id))
                        .map(ModuleNode::name)
                        .findFirst()
                        .orElse(id),
                    "EVENT_LISTENER"
                ))
                .toList(),
            downstream.size()
        );
    }

    private int calculateIndirectConsumers(List<AffectedModule> affected) {
        // Simplified: for now, return 0
        // In future, traverse dependencies to find modules that depend on listeners
        return 0;
    }
}
```

### REST Controller (in eventus-spring)

```java
// io.eventus.spring.impact/ImpactAnalysisController.java
@RestController
@RequestMapping("/eventus/api/impact")
public class ImpactAnalysisController {
    private final ImpactAnalyzer impactAnalyzer;

    public ImpactAnalysisController(ImpactAnalyzer impactAnalyzer) {
        this.impactAnalyzer = impactAnalyzer;
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<EventImpactResponse> analyzeEvent(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(impactAnalyzer.analyzeEventImpact(eventId));
        } catch (EventNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/module/{moduleId}")
    public ResponseEntity<ModuleImpactResponse> analyzeModule(@PathVariable String moduleId) {
        try {
            return ResponseEntity.ok(impactAnalyzer.analyzeModuleImpact(moduleId));
        } catch (ModuleNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

### Update Auto-Configuration

```java
// In EventusAutoConfiguration
@Bean
@ConditionalOnMissingBean
public ImpactAnalyzer impactAnalyzer(GraphReader reader) {
    return new InMemoryImpactAnalyzer(reader);
}

@Bean
@ConditionalOnMissingBean
public ImpactAnalysisController impactAnalysisController(ImpactAnalyzer analyzer) {
    return new ImpactAnalysisController(analyzer);
}
```

## Tests Required

```java
// eventus-core/src/test/java/io/eventus/core/impact/InMemoryImpactAnalyzerTest.java
class InMemoryImpactAnalyzerTest {
    private InMemoryImpactAnalyzer analyzer;
    private GraphModel graphModel;

    @BeforeEach
    void setup() {
        // Build test graph with order → inventory, order → notification
        graphModel = new GraphModel();
        // ... populate with modules and events
        analyzer = new InMemoryImpactAnalyzer(/* reader from model */);
    }

    @Test
    void analyzeEventImpactReturnsAllListeners() {
        EventImpactResponse response = analyzer.analyzeEventImpact("OrderPlaced");
        assertThat(response.affectedModules()).hasSize(2);
        assertThat(response.affectedModules().stream().map(AffectedModule::moduleId))
            .containsExactlyInAnyOrder("inventory", "notification");
    }

    @Test
    void analyzeModuleImpactReturnsPublishedEvents() {
        ModuleImpactResponse response = analyzer.analyzeModuleImpact("order");
        assertThat(response.publishedEvents()).hasSize(1);
        assertThat(response.totalAffectedModules()).isEqualTo(2);
    }

    @Test
    void throwsEventNotFoundExceptionForInvalidEventId() {
        assertThatThrownBy(() -> analyzer.analyzeEventImpact("NonExistent"))
            .isInstanceOf(EventNotFoundException.class);
    }
}

// eventus-spring/src/test/java/io/eventus/spring/impact/ImpactAnalysisControllerTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class ImpactAnalysisControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void eventImpactEndpointReturnsAffectedModules() throws Exception {
        mockMvc.perform(get("/eventus/api/impact/event/OrderPlaced"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.affectedModules", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void eventImpactEndpointReturns404ForUnknownEvent() throws Exception {
        mockMvc.perform(get("/eventus/api/impact/event/UnknownEvent"))
            .andExpect(status().isNotFound());
    }

    @Test
    void moduleImpactEndpointReturnsPublishedEvents() throws Exception {
        mockMvc.perform(get("/eventus/api/impact/module/order"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publishedEvents", isArray()));
    }
}
```

## Done When
- Both endpoints respond correctly with proper structure
- Edge cases handled (404 for missing IDs, empty arrays when no relationships)
- All tests green
- Performance verified: queries <200ms on realistic graphs
