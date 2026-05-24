# S24 — Generic JVM Extractor: AnnotationBasedExtractor + Auto-Configuration

## Goal
Implement `eventus-generic` — a classpath-scanning `EventGraphExtractor` that reads `@EventModule`, `@Publishes`, and `@Listens` annotations (from S23) to build a graph for any JVM application without a Spring Modulith or Kafka dependency.

## Acceptance Criteria
- [ ] `eventus-generic` module exists and is declared in root `pom.xml`
- [ ] `AnnotationBasedExtractor` scans user-specified base packages for annotated classes
- [ ] `@EventModule` classes become `ModuleNode` instances
- [ ] `@Publishes` methods create `EventNode` instances and PUBLISHES edges
- [ ] `@Listens` methods create LISTENS_TO edges
- [ ] Module name defaults to simple class name when `@EventModule(name="")` is blank
- [ ] Event IDs are the fully-qualified class name of the event type
- [ ] Auto-configuration registers `AnnotationBasedExtractor` when base packages are configured
- [ ] Unit tests cover scan + graph construction with no Spring context
- [ ] `mvn verify` passes

## Module Structure

```
eventus-generic/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── io/eventus/generic/
    │   │       ├── AnnotationBasedExtractor.java
    │   │       └── autoconfigure/
    │   │           └── EventusGenericAutoConfiguration.java
    │   └── resources/
    │       └── META-INF/spring/
    │           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── test/
        └── java/
            └── io/eventus/generic/
                └── AnnotationBasedExtractorTest.java
```

## Module POM

```xml
<artifactId>eventus-generic</artifactId>
<name>Eventus Generic</name>
<description>Annotation-based event graph extractor for plain JVM applications without Spring Modulith</description>

<dependencies>
  <dependency>
    <groupId>io.eventus</groupId>
    <artifactId>eventus-core</artifactId>
    <version>${project.version}</version>
  </dependency>

  <!-- Classpath scanning — ClassGraph is lightweight (no transitive deps) -->
  <dependency>
    <groupId>io.github.classgraph</groupId>
    <artifactId>classgraph</artifactId>
    <version>4.8.174</version>
  </dependency>

  <!-- Optional Spring Boot auto-config -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-autoconfigure</artifactId>
    <optional>true</optional>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

## Implementation

```java
// io.eventus.generic/AnnotationBasedExtractor.java
public class AnnotationBasedExtractor implements EventGraphExtractor {
    private final List<String> basePackages;

    public AnnotationBasedExtractor(List<String> basePackages) {
        this.basePackages = List.copyOf(basePackages);
    }

    @Override
    public GraphModel extract() {
        List<ModuleNode> modules = new ArrayList<>();
        List<EventNode> events = new ArrayList<>();
        List<EventEdge> edges = new ArrayList<>();

        try (ScanResult scan = new ClassGraph()
            .acceptPackages(basePackages.toArray(String[]::new))
            .enableAllInfo()
            .scan()) {

            // Collect module classes
            Map<String, String> classToModuleId = new HashMap<>();
            for (ClassInfo classInfo : scan.getClassesWithAnnotation(EventModule.class.getName())) {
                Class<?> clazz = classInfo.loadClass();
                EventModule ann = clazz.getAnnotation(EventModule.class);
                String moduleId = ann.name().isBlank() ? clazz.getSimpleName() : ann.name();
                classToModuleId.put(clazz.getName(), moduleId);
                modules.add(new ModuleNode(moduleId, moduleId, 0, 0, ModuleStatus.UNKNOWN));
            }

            // Collect events and edges from @Publishes/@Listens methods
            for (ClassInfo classInfo : scan.getClassesWithAnnotation(EventModule.class.getName())) {
                Class<?> clazz = classInfo.loadClass();
                String publisherModuleId = classToModuleId.get(clazz.getName());

                for (Method method : clazz.getDeclaredMethods()) {
                    Publishes pub = method.getAnnotation(Publishes.class);
                    if (pub != null) {
                        for (Class<?> eventType : pub.value()) {
                            String eventId = eventType.getName();
                            events.add(new EventNode(eventId, eventType.getSimpleName(), publisherModuleId));
                            edges.add(new EventEdge(
                                publisherModuleId + "_publishes_" + eventId,
                                publisherModuleId, eventId, null, EdgeType.PUBLISHES
                            ));
                        }
                    }

                    Listens listens = method.getAnnotation(Listens.class);
                    if (listens != null) {
                        for (Class<?> eventType : listens.value()) {
                            String eventId = eventType.getName();
                            edges.add(new EventEdge(
                                publisherModuleId + "_listens_" + eventId,
                                null, eventId, publisherModuleId, EdgeType.LISTENS_TO
                            ));
                        }
                    }
                }
            }
        }

        return new GraphModel(
            List.copyOf(modules),
            deduplicate(events),
            List.copyOf(edges)
        );
    }

    private List<EventNode> deduplicate(List<EventNode> events) {
        Map<String, EventNode> seen = new LinkedHashMap<>();
        for (EventNode e : events) {
            seen.putIfAbsent(e.id(), e);
        }
        return List.copyOf(seen.values());
    }
}
```

## Auto-Configuration

```java
// io.eventus.generic.autoconfigure/EventusGenericAutoConfiguration.java
@Configuration
@ConditionalOnProperty(prefix = "eventus.generic", name = "base-packages")
@AutoConfiguration
public class EventusGenericAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventGraphExtractor.class)
    public EventGraphExtractor annotationBasedExtractor(
        @Value("${eventus.generic.base-packages}") String basePackages
    ) {
        List<String> packages = Arrays.stream(basePackages.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
        return new AnnotationBasedExtractor(packages);
    }
}
```

## Configuration

```properties
# Comma-separated list of base packages to scan
eventus.generic.base-packages=com.example.order,com.example.inventory
```

## Tests Required

```java
// Test fixtures — annotated classes in the test package
@EventModule(name = "orders")
class OrderModule {
    @Publishes(OrderPlaced.class)
    public void placeOrder() {}
}

@EventModule(name = "inventory")
class InventoryModule {
    @Listens(OrderPlaced.class)
    public void onOrderPlaced(OrderPlaced event) {}
}

record OrderPlaced(String id) {}

class AnnotationBasedExtractorTest {
    private final AnnotationBasedExtractor extractor =
        new AnnotationBasedExtractor(List.of("io.eventus.generic"));

    @Test
    void extractsModules() {
        GraphModel graph = extractor.extract();
        assertThat(graph.modules()).extracting(ModuleNode::id)
            .contains("orders", "inventory");
    }

    @Test
    void extractsEvents() {
        GraphModel graph = extractor.extract();
        assertThat(graph.events()).extracting(EventNode::name)
            .contains("OrderPlaced");
    }

    @Test
    void extractsPublishesEdge() {
        GraphModel graph = extractor.extract();
        assertThat(graph.edges())
            .anyMatch(e -> e.fromModuleId().equals("orders") && e.type() == EdgeType.PUBLISHES);
    }

    @Test
    void extractsListensEdge() {
        GraphModel graph = extractor.extract();
        assertThat(graph.edges())
            .anyMatch(e -> e.toModuleId().equals("inventory") && e.type() == EdgeType.LISTENS_TO);
    }

    @Test
    void deduplicatesEventsPublishedByMultipleMethods() {
        // Two @Publishes on different methods for the same event type → one EventNode
        GraphModel graph = extractor.extract();
        long orderPlacedCount = graph.events().stream()
            .filter(e -> e.name().equals("OrderPlaced"))
            .count();
        assertThat(orderPlacedCount).isEqualTo(1);
    }
}
```

## Done When
- `AnnotationBasedExtractor` scans packages and builds a correct `GraphModel`
- No Spring context required to run the extractor
- Auto-configuration activates only when `eventus.generic.base-packages` is set
- All tests pass without a running Spring app
- `mvn verify` green
