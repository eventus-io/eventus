# S13 — Violation Detection & Architectural Rules

## Goal
Detect and report architectural violations: circular event chains, hidden coupling, consistently failing listeners, and other anti-patterns.

## Acceptance Criteria
- [ ] Detects circular event dependencies (A → B → A)
- [ ] Detects hidden coupling: module C listens to A but doesn't declare dependency on A
- [ ] Detects consistently failing listeners (>80% failure rate over last 24h)
- [ ] Detects unused events: published but no listeners
- [ ] Each violation has severity level (ERROR, WARNING, INFO)
- [ ] REST endpoint `/eventus/api/violations` returns structured list
- [ ] Violations cached for 5 minutes to avoid repeated analysis
- [ ] All tests green

## Violation Types

```java
// io.eventus.core.violations/
public enum ViolationType {
    CIRCULAR_EVENT_DEPENDENCY("Circular event chain detected"),
    HIDDEN_COUPLING("Module has listener relationship but no declared dependency"),
    UNUSED_EVENT("Event published but has no listeners"),
    FAILING_LISTENER("Listener consistently fails (>80% error rate)"),
    STALE_PUBLICATION("Event publication incomplete for extended period");
    
    private final String description;
    // constructor
}

public enum ViolationSeverity {
    ERROR,
    WARNING,
    INFO
}

public record Violation(
    String id,
    ViolationType type,
    ViolationSeverity severity,
    String title,
    String description,
    List<String> affectedModuleIds,
    List<String> affectedEventIds,
    long detectedAt
) {}
```

## Violation Analyzer Service

```java
// io.eventus.core.violations/ViolationAnalyzer.java
public interface ViolationAnalyzer {
    List<Violation> analyze();
}

// io.eventus.core.violations/InMemoryViolationAnalyzer.java
public class InMemoryViolationAnalyzer implements ViolationAnalyzer {
    private final GraphReader graphReader;
    private volatile List<Violation> cachedViolations;
    private volatile long lastAnalyzedAt = 0;
    private static final Duration CACHE_DURATION = Duration.ofMinutes(5);

    public InMemoryViolationAnalyzer(GraphReader graphReader) {
        this.graphReader = graphReader;
    }

    @Override
    public List<Violation> analyze() {
        if (shouldUseCache()) {
            return cachedViolations;
        }

        List<Violation> violations = new ArrayList<>();
        violations.addAll(detectCircularDependencies());
        violations.addAll(detectHiddenCoupling());
        violations.addAll(detectUnusedEvents());
        violations.addAll(detectFailingListeners());
        violations.addAll(detectStalePublications());

        cachedViolations = Collections.unmodifiableList(violations);
        lastAnalyzedAt = System.currentTimeMillis();
        return cachedViolations;
    }

    private List<Violation> detectCircularDependencies() {
        List<Violation> violations = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> currentPath = new HashSet<>();

        for (EventNode event : graphReader.getEvents()) {
            if (hasCircularPath(event.id(), event.id(), visited, currentPath)) {
                violations.add(new Violation(
                    UUID.randomUUID().toString(),
                    ViolationType.CIRCULAR_EVENT_DEPENDENCY,
                    ViolationSeverity.ERROR,
                    "Circular event chain: " + event.name(),
                    "This event and its listeners form a circular dependency",
                    List.of(event.publisherModuleId()),
                    List.of(event.id()),
                    System.currentTimeMillis()
                ));
            }
        }
        return violations;
    }

    private boolean hasCircularPath(String startEventId, String currentEventId, 
                                     Set<String> visited, Set<String> currentPath) {
        if (currentPath.contains(currentEventId)) {
            return true;
        }
        if (visited.contains(currentEventId)) {
            return false;
        }

        visited.add(currentEventId);
        currentPath.add(currentEventId);

        List<EventEdge> edges = graphReader.getEdgesForEvent(currentEventId);
        for (EventEdge edge : edges) {
            if (edge.edgeType() == EdgeType.LISTENS_TO) {
                // Find events published by the listening module
                List<EventNode> eventsFromListener = graphReader.getEvents().stream()
                    .filter(e -> e.publisherModuleId().equals(edge.toModuleId()))
                    .toList();
                for (EventNode event : eventsFromListener) {
                    if (hasCircularPath(startEventId, event.id(), visited, currentPath)) {
                        return true;
                    }
                }
            }
        }

        currentPath.remove(currentEventId);
        return false;
    }

    private List<Violation> detectHiddenCoupling() {
        List<Violation> violations = new ArrayList<>();
        
        for (ModuleNode module : graphReader.getModules()) {
            List<EventEdge> listenerEdges = graphReader.getEdges().stream()
                .filter(e -> e.toModuleId().equals(module.id()) && e.edgeType() == EdgeType.LISTENS_TO)
                .toList();

            for (EventEdge edge : listenerEdges) {
                // Check if module explicitly declares dependency on publisher
                boolean hasDeclaredDependency = graphReader.getEdges().stream()
                    .anyMatch(e -> e.fromModuleId().equals(module.id()) && 
                                  e.toModuleId().equals(edge.fromModuleId()));
                
                if (!hasDeclaredDependency) {
                    violations.add(new Violation(
                        UUID.randomUUID().toString(),
                        ViolationType.HIDDEN_COUPLING,
                        ViolationSeverity.WARNING,
                        "Hidden coupling: " + module.name() + " → " + edge.fromModuleId(),
                        module.name() + " listens to events from " + edge.fromModuleId() + 
                            " but does not declare a dependency",
                        List.of(module.id(), edge.fromModuleId()),
                        List.of(edge.eventId()),
                        System.currentTimeMillis()
                    ));
                }
            }
        }
        return violations;
    }

    private List<Violation> detectUnusedEvents() {
        List<Violation> violations = new ArrayList<>();
        
        for (EventNode event : graphReader.getEvents()) {
            List<EventEdge> listeners = graphReader.getEdgesForEvent(event.id()).stream()
                .filter(e -> e.edgeType() == EdgeType.LISTENS_TO)
                .toList();

            if (listeners.isEmpty()) {
                violations.add(new Violation(
                    UUID.randomUUID().toString(),
                    ViolationType.UNUSED_EVENT,
                    ViolationSeverity.INFO,
                    "Unused event: " + event.name(),
                    event.name() + " is published but has no listeners",
                    List.of(event.publisherModuleId()),
                    List.of(event.id()),
                    System.currentTimeMillis()
                ));
            }
        }
        return violations;
    }

    private List<Violation> detectFailingListeners() {
        List<Violation> violations = new ArrayList<>();
        
        for (PublicationRecord publication : graphReader.getPublications()) {
            if (publication.status() == PublicationStatus.INCOMPLETE) {
                long incompleteCount = graphReader.getPublications().stream()
                    .filter(p -> p.listenerName().equals(publication.listenerName()) && 
                               p.status() == PublicationStatus.INCOMPLETE)
                    .count();
                long totalCount = graphReader.getPublications().stream()
                    .filter(p -> p.listenerName().equals(publication.listenerName()))
                    .count();

                if (totalCount > 0 && (incompleteCount * 100.0 / totalCount) > 80) {
                    violations.add(new Violation(
                        UUID.randomUUID().toString(),
                        ViolationType.FAILING_LISTENER,
                        ViolationSeverity.ERROR,
                        "Failing listener: " + publication.listenerName(),
                        publication.listenerName() + " has >80% failure rate",
                        List.of(publication.moduleId()),
                        List.of(publication.eventType()),
                        System.currentTimeMillis()
                    ));
                }
            }
        }
        return violations;
    }

    private List<Violation> detectStalePublications() {
        List<Violation> violations = new ArrayList<>();
        Instant staleThreshold = Instant.now().minus(Duration.ofHours(2));
        
        for (PublicationRecord publication : graphReader.getPublications()) {
            if (publication.status() == PublicationStatus.STALE && 
                publication.publishedAt().isBefore(staleThreshold)) {
                violations.add(new Violation(
                    UUID.randomUUID().toString(),
                    ViolationType.STALE_PUBLICATION,
                    ViolationSeverity.ERROR,
                    "Stale publication: " + publication.eventType(),
                    "Event publication for " + publication.listenerName() + 
                        " is stale (pending since " + publication.publishedAt() + ")",
                    List.of(publication.moduleId()),
                    List.of(publication.eventType()),
                    System.currentTimeMillis()
                ));
            }
        }
        return violations;
    }

    private boolean shouldUseCache() {
        return cachedViolations != null && 
               (System.currentTimeMillis() - lastAnalyzedAt) < CACHE_DURATION.toMillis();
    }
}
```

## REST Controller

```java
// io.eventus.spring.violations/ViolationsController.java
@RestController
@RequestMapping("/eventus/api/violations")
public class ViolationsController {
    private final ViolationAnalyzer violationAnalyzer;

    public ViolationsController(ViolationAnalyzer violationAnalyzer) {
        this.violationAnalyzer = violationAnalyzer;
    }

    @GetMapping
    public List<Violation> getViolations() {
        return violationAnalyzer.analyze();
    }

    @GetMapping(params = "severity")
    public List<Violation> getViolationsBySeverity(@RequestParam ViolationSeverity severity) {
        return violationAnalyzer.analyze().stream()
            .filter(v -> v.severity() == severity)
            .toList();
    }

    @GetMapping(params = "type")
    public List<Violation> getViolationsByType(@RequestParam ViolationType type) {
        return violationAnalyzer.analyze().stream()
            .filter(v -> v.type() == type)
            .toList();
    }
}
```

## Update Auto-Configuration

```java
// In EventusAutoConfiguration
@Bean
@ConditionalOnMissingBean
public ViolationAnalyzer violationAnalyzer(GraphReader graphReader) {
    return new InMemoryViolationAnalyzer(graphReader);
}

@Bean
@ConditionalOnMissingBean
public ViolationsController violationsController(ViolationAnalyzer analyzer) {
    return new ViolationsController(analyzer);
}
```

## Tests Required

```java
// eventus-core/src/test/java/io/eventus/core/violations/InMemoryViolationAnalyzerTest.java
class InMemoryViolationAnalyzerTest {
    private InMemoryViolationAnalyzer analyzer;

    @BeforeEach
    void setup() {
        // Build graph with violations
        analyzer = new InMemoryViolationAnalyzer(/* graphReader with violations */);
    }

    @Test
    void detectsCircularEventDependencies() {
        // Setup: Order → OrderPlaced (listened by Inventory) → InventoryUpdated (listened by Order)
        List<Violation> violations = analyzer.analyze();
        assertThat(violations).anyMatch(v -> v.type() == ViolationType.CIRCULAR_EVENT_DEPENDENCY);
    }

    @Test
    void detectsHiddenCoupling() {
        // Setup: Module A listens to events from Module B but doesn't declare DEPENDS_ON
        List<Violation> violations = analyzer.analyze();
        assertThat(violations).anyMatch(v -> v.type() == ViolationType.HIDDEN_COUPLING);
    }

    @Test
    void detectsUnusedEvents() {
        // Setup: Event with no listeners
        List<Violation> violations = analyzer.analyze();
        assertThat(violations).anyMatch(v -> v.type() == ViolationType.UNUSED_EVENT);
    }

    @Test
    void cachesDurationFiveMinutes() {
        List<Violation> first = analyzer.analyze();
        List<Violation> second = analyzer.analyze();
        assertThat(first).isEqualTo(second); // Same object reference due to caching
    }
}

// eventus-spring/src/test/java/io/eventus/spring/violations/ViolationsControllerTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class ViolationsControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void violationsEndpointReturnsStructuredList() throws Exception {
        mockMvc.perform(get("/eventus/api/violations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", isArray()));
    }

    @Test
    void violationsCanBeFilteredBySeverity() throws Exception {
        mockMvc.perform(get("/eventus/api/violations?severity=ERROR"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].severity", everyItem(equalTo("ERROR"))));
    }

    @Test
    void violationsCanBeFilteredByType() throws Exception {
        mockMvc.perform(get("/eventus/api/violations?type=UNUSED_EVENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].type", everyItem(equalTo("UNUSED_EVENT"))));
    }
}
```

## Done When
- All violation types detected correctly
- Controller exposes REST endpoint with filtering
- Cache working (5-minute TTL)
- All tests green
- README updated with violation types and examples
