# S15 — Architectural Drift Detection

## Goal
Compare the current runtime graph against a committed baseline to detect when the codebase has drifted from its intended architecture.

## Acceptance Criteria
- [ ] Save current graph as JSON baseline to `.eventus/baseline.json`
- [ ] Load baseline from classpath at startup
- [ ] REST endpoint `/eventus/api/drift` compares current → baseline
- [ ] Reports added/removed modules, events, edges
- [ ] Each drift item has severity (BREAKING, MODERATE, MINOR)
- [ ] Drift can be ignored or applied as new baseline
- [ ] All tests green

## Drift Detection Data Model

```java
// io.eventus.core.drift/
public enum DriftType {
    MODULE_ADDED,
    MODULE_REMOVED,
    EVENT_ADDED,
    EVENT_REMOVED,
    LISTENER_ADDED,
    LISTENER_REMOVED,
    PUBLISHER_CHANGED
}

public enum DriftSeverity {
    BREAKING,      // Removed module/event that others depend on
    MODERATE,      // New module/event, listeners added/removed
    MINOR          // Rename, reordering
}

public record Drift(
    String id,
    DriftType type,
    DriftSeverity severity,
    String title,
    String description,
    String affectedItemId,
    String affectedItemName,
    long detectedAt
) {}

public record ArchitecturalDriftReport(
    List<Drift> drifts,
    int totalDrifts,
    int breachingCount,
    long comparedAt
) {}

public record BaselineSnapshot(
    List<ModuleNode> modules,
    List<EventNode> events,
    List<EventEdge> edges,
    long capturedAt
) {}
```

## Baseline Management Service

```java
// io.eventus.core.drift/BaselineManager.java
public interface BaselineManager {
    BaselineSnapshot loadBaseline();
    void saveBaseline(GraphModel current);
    boolean hasBaseline();
}

// io.eventus.core.drift/FileSystemBaselineManager.java
public class FileSystemBaselineManager implements BaselineManager {
    private final Path baselinePath;
    private final ObjectMapper mapper;

    public FileSystemBaselineManager(Path baselineDir) {
        this.baselinePath = baselineDir.resolve("baseline.json");
        this.mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    @Override
    public BaselineSnapshot loadBaseline() {
        if (!Files.exists(baselinePath)) {
            return null;
        }
        try {
            return mapper.readValue(baselinePath.toFile(), BaselineSnapshot.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load baseline: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveBaseline(GraphModel current) {
        try {
            Files.createDirectories(baselinePath.getParent());
            BaselineSnapshot snapshot = new BaselineSnapshot(
                current.modules(),
                current.events(),
                current.edges(),
                System.currentTimeMillis()
            );
            mapper.writerWithDefaultPrettyPrinter()
                .writeValue(baselinePath.toFile(), snapshot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save baseline: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasBaseline() {
        return Files.exists(baselinePath);
    }
}
```

### Drift Analyzer

```java
// io.eventus.core.drift/DriftAnalyzer.java
public interface DriftAnalyzer {
    ArchitecturalDriftReport analyzeDrift();
}

// io.eventus.core.drift/InMemoryDriftAnalyzer.java
public class InMemoryDriftAnalyzer implements DriftAnalyzer {
    private final GraphReader currentGraph;
    private final BaselineManager baselineManager;

    public InMemoryDriftAnalyzer(GraphReader currentGraph, BaselineManager baselineManager) {
        this.currentGraph = currentGraph;
        this.baselineManager = baselineManager;
    }

    @Override
    public ArchitecturalDriftReport analyzeDrift() {
        BaselineSnapshot baseline = baselineManager.loadBaseline();
        if (baseline == null) {
            return new ArchitecturalDriftReport(List.of(), 0, 0, System.currentTimeMillis());
        }

        List<Drift> drifts = new ArrayList<>();
        drifts.addAll(detectModuleChanges(baseline));
        drifts.addAll(detectEventChanges(baseline));
        drifts.addAll(detectEdgeChanges(baseline));

        int breachingCount = (int) drifts.stream()
            .filter(d -> d.severity() == DriftSeverity.BREAKING)
            .count();

        return new ArchitecturalDriftReport(drifts, drifts.size(), breachingCount, System.currentTimeMillis());
    }

    private List<Drift> detectModuleChanges(BaselineSnapshot baseline) {
        List<Drift> drifts = new ArrayList<>();

        Set<String> baselineModuleIds = baseline.modules().stream()
            .map(ModuleNode::id)
            .collect(Collectors.toSet());
        Set<String> currentModuleIds = currentGraph.getModules().stream()
            .map(ModuleNode::id)
            .collect(Collectors.toSet());

        // Detect removed modules
        baselineModuleIds.stream()
            .filter(id -> !currentModuleIds.contains(id))
            .forEach(id -> {
                String moduleName = baseline.modules().stream()
                    .filter(m -> m.id().equals(id))
                    .map(ModuleNode::name)
                    .findFirst()
                    .orElse(id);

                // Check if any module depends on this one
                boolean isDepended = baseline.edges().stream()
                    .anyMatch(e -> e.toModuleId().equals(id) && 
                                  e.edgeType() != EdgeType.DEPENDS_ON);

                DriftSeverity severity = isDepended ? DriftSeverity.BREAKING : DriftSeverity.MODERATE;

                drifts.add(new Drift(
                    UUID.randomUUID().toString(),
                    DriftType.MODULE_REMOVED,
                    severity,
                    "Module removed: " + moduleName,
                    moduleName + " was in baseline but is no longer present",
                    id,
                    moduleName,
                    System.currentTimeMillis()
                ));
            });

        // Detect added modules
        currentModuleIds.stream()
            .filter(id -> !baselineModuleIds.contains(id))
            .forEach(id -> {
                String moduleName = currentGraph.getModules().stream()
                    .filter(m -> m.id().equals(id))
                    .map(ModuleNode::name)
                    .findFirst()
                    .orElse(id);

                drifts.add(new Drift(
                    UUID.randomUUID().toString(),
                    DriftType.MODULE_ADDED,
                    DriftSeverity.MODERATE,
                    "Module added: " + moduleName,
                    moduleName + " is new since baseline",
                    id,
                    moduleName,
                    System.currentTimeMillis()
                ));
            });

        return drifts;
    }

    private List<Drift> detectEventChanges(BaselineSnapshot baseline) {
        List<Drift> drifts = new ArrayList<>();

        Set<String> baselineEventIds = baseline.events().stream()
            .map(EventNode::id)
            .collect(Collectors.toSet());
        Set<String> currentEventIds = currentGraph.getEvents().stream()
            .map(EventNode::id)
            .collect(Collectors.toSet());

        // Detect removed events
        baselineEventIds.stream()
            .filter(id -> !currentEventIds.contains(id))
            .forEach(id -> {
                String eventName = baseline.events().stream()
                    .filter(e -> e.id().equals(id))
                    .map(EventNode::name)
                    .findFirst()
                    .orElse(id);

                // Check if any listener depends on this event
                boolean isListened = baseline.edges().stream()
                    .anyMatch(e -> e.eventId().equals(id) && 
                                  e.edgeType() == EdgeType.LISTENS_TO);

                DriftSeverity severity = isListened ? DriftSeverity.BREAKING : DriftSeverity.MODERATE;

                drifts.add(new Drift(
                    UUID.randomUUID().toString(),
                    DriftType.EVENT_REMOVED,
                    severity,
                    "Event removed: " + eventName,
                    eventName + " was published in baseline but is no longer",
                    id,
                    eventName,
                    System.currentTimeMillis()
                ));
            });

        // Detect added events
        currentEventIds.stream()
            .filter(id -> !baselineEventIds.contains(id))
            .forEach(id -> {
                String eventName = currentGraph.getEvents().stream()
                    .filter(e -> e.id().equals(id))
                    .map(EventNode::name)
                    .findFirst()
                    .orElse(id);

                drifts.add(new Drift(
                    UUID.randomUUID().toString(),
                    DriftType.EVENT_ADDED,
                    DriftSeverity.MINOR,
                    "Event added: " + eventName,
                    eventName + " is new since baseline",
                    id,
                    eventName,
                    System.currentTimeMillis()
                ));
            });

        return drifts;
    }

    private List<Drift> detectEdgeChanges(BaselineSnapshot baseline) {
        List<Drift> drifts = new ArrayList<>();

        Set<String> baselineEdgeIds = baseline.edges().stream()
            .map(EventEdge::id)
            .collect(Collectors.toSet());
        Set<String> currentEdgeIds = currentGraph.getEdges().stream()
            .map(EventEdge::id)
            .collect(Collectors.toSet());

        baselineEdgeIds.stream()
            .filter(id -> !currentEdgeIds.contains(id))
            .forEach(id -> {
                EventEdge edge = baseline.edges().stream()
                    .filter(e -> e.id().equals(id))
                    .findFirst()
                    .get();

                drifts.add(new Drift(
                    UUID.randomUUID().toString(),
                    DriftType.LISTENER_REMOVED,
                    DriftSeverity.MODERATE,
                    "Listener removed: " + edge.toModuleId(),
                    edge.toModuleId() + " no longer listens to this event",
                    edge.eventId(),
                    edge.eventId(),
                    System.currentTimeMillis()
                ));
            });

        currentEdgeIds.stream()
            .filter(id -> !baselineEdgeIds.contains(id))
            .forEach(id -> {
                EventEdge edge = currentGraph.getEdges().stream()
                    .filter(e -> e.id().equals(id))
                    .findFirst()
                    .get();

                drifts.add(new Drift(
                    UUID.randomUUID().toString(),
                    DriftType.LISTENER_ADDED,
                    DriftSeverity.MINOR,
                    "Listener added: " + edge.toModuleId(),
                    edge.toModuleId() + " now listens to this event",
                    edge.eventId(),
                    edge.eventId(),
                    System.currentTimeMillis()
                ));
            });

        return drifts;
    }
}
```

## REST Controller

```java
// io.eventus.spring.drift/DriftController.java
@RestController
@RequestMapping("/eventus/api/drift")
public class DriftController {
    private final DriftAnalyzer driftAnalyzer;
    private final BaselineManager baselineManager;
    private final GraphReader graphReader;

    @GetMapping
    public ArchitecturalDriftReport getDrift() {
        return driftAnalyzer.analyzeDrift();
    }

    @PostMapping("/baseline")
    public ResponseEntity<Void> captureBaseline() {
        baselineManager.saveBaseline(/* construct from graphReader */);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/baseline")
    public ResponseEntity<BaselineSnapshot> getBaseline() {
        BaselineSnapshot baseline = baselineManager.loadBaseline();
        if (baseline == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(baseline);
    }
}
```

## Update Auto-Configuration

```java
// In EventusAutoConfiguration
@Bean
@ConditionalOnMissingBean
public BaselineManager baselineManager() {
    Path baselineDir = Paths.get(System.getProperty("user.dir"), ".eventus");
    return new FileSystemBaselineManager(baselineDir);
}

@Bean
@ConditionalOnMissingBean
public DriftAnalyzer driftAnalyzer(GraphReader graphReader, BaselineManager baselineManager) {
    return new InMemoryDriftAnalyzer(graphReader, baselineManager);
}

@Bean
@ConditionalOnMissingBean
public DriftController driftController(DriftAnalyzer analyzer, BaselineManager baselineManager, GraphReader graphReader) {
    return new DriftController(analyzer, baselineManager, graphReader);
}
```

## Tests Required

```java
// eventus-core/src/test/java/io/eventus/core/drift/InMemoryDriftAnalyzerTest.java
class InMemoryDriftAnalyzerTest {
    private InMemoryDriftAnalyzer analyzer;
    private BaselineSnapshot baseline;

    @Test
    void detectsRemovedModule() {
        // Baseline has order + inventory; current has only inventory
        ArchitecturalDriftReport report = analyzer.analyzeDrift();
        
        assertThat(report.drifts()).anyMatch(d -> 
            d.type() == DriftType.MODULE_REMOVED && 
            d.severity() == DriftSeverity.BREAKING
        );
    }

    @Test
    void detectsAddedEvent() {
        // Baseline has OrderPlaced; current has OrderPlaced + OrderCancelled
        ArchitecturalDriftReport report = analyzer.analyzeDrift();
        
        assertThat(report.drifts()).anyMatch(d -> 
            d.type() == DriftType.EVENT_ADDED && 
            d.affectedItemName().equals("OrderCancelled")
        );
    }

    @Test
    void removedEventWithListenersIsBREAKING() {
        // Baseline has event with listeners; current doesn't
        ArchitecturalDriftReport report = analyzer.analyzeDrift();
        
        assertThat(report.breachingCount()).isGreaterThan(0);
    }
}

// eventus-spring/src/test/java/io/eventus/spring/drift/DriftControllerTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class DriftControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void driftEndpointReturnsReport() throws Exception {
        mockMvc.perform(get("/eventus/api/drift"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.drifts", isArray()));
    }

    @Test
    void captureBaselineEndpointSavesSnapshot() throws Exception {
        mockMvc.perform(post("/eventus/api/drift/baseline"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/eventus/api/drift/baseline"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.modules", isArray()));
    }
}
```

## Done When
- Baseline can be loaded/saved
- Drift analyzer detects all change types correctly
- Severity classification working (BREAKING → modules/events with dependents)
- REST endpoints working: GET drift, POST/GET baseline
- All tests green
- README updated with drift detection usage
