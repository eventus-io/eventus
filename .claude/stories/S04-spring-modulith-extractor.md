# S04 — Spring Modulith Extractor

## Goal
Implement `SpringModulithExtractor` in `eventus-spring`. This is the core value
of v0.1 — reading the live module and event model from a Spring Modulith application.

## Acceptance Criteria
- [ ] `SpringModulithExtractor` implements `EventGraphExtractor`
- [ ] Reads all modules from `ApplicationModules`
- [ ] For each module: creates a `ModuleNode` with correct name, beanCount, aggregateCount
- [ ] For each event type published by a module: creates an `EventNode` + `PUBLISHES` edge
- [ ] For each event type listened to by a module: creates a `LISTENS_TO` edge
- [ ] If `EventPublicationRepository` is present in context:
      reads incomplete publications → sets affected module status to `WARNING`
      reads stale publications (age > configurable threshold) → sets status to `ERROR`
- [ ] If `EventPublicationRepository` is NOT present: all modules default to `HEALTHY`
- [ ] `SpringModulithExtractor` is annotated with nothing — plain class, Spring wires it
- [ ] Works when Spring Modulith is on the classpath; fails gracefully if not

## Key Spring Modulith APIs to Use

```java
// Get the module model (requires the main application class)
ApplicationModules modules = ApplicationModules.of(MyApp.class);

// Iterate modules
modules.forEach(module -> {
    module.getName();                    // module name
    module.getSpringBeans().size();      // bean count
    // published events: module.getPublishedEvents()  → Set<EventType>
    // listened events: derive from @EventListener methods in module
});
```

For published/listened events, use:
```java
// ApplicationModule has:
module.getPublishedEvents()   // returns types this module publishes
module.getDependencies(modules, DependencyType.EVENT_LISTENER)
// also inspect via module.getType() and look for @EventListener methods
```

Refer to Spring Modulith docs for exact API — version may vary.
Use `module.getDisplayName()` for the human-readable name.

## EventPublicationRepository Integration

```java
// Inject optionally
@Autowired(required = false)
private EventPublicationRepository publicationRepository;

// If present:
publicationRepository.findIncompletePublications()
    .forEach(pub -> {
        // derive moduleId from pub.getListenerClass() package
        // mark that module as WARNING
    });
```

Stale threshold: configurable via property
`eventus.publications.stale-threshold=PT2H` (default: 2 hours, ISO-8601 duration)

## Constructor Signature

```java
public class SpringModulithExtractor implements EventGraphExtractor {

    private final Class<?> applicationClass;
    private final EventPublicationRepository publicationRepository; // nullable
    private final Duration staleThreshold;

    public SpringModulithExtractor(
        Class<?> applicationClass,
        @Nullable EventPublicationRepository publicationRepository,
        Duration staleThreshold
    ) { ... }
}
```

The `applicationClass` is the `@SpringBootApplication` class.
Auto-configuration will supply it via `SpringApplication` context.

## Package Structure

```
io.eventus.spring/
├── EventusSpringExtractor.java        (the extractor)
└── model/
    └── (no additional model — use eventus-core model)
```

## Error Handling
- If `ApplicationModules.of()` throws (e.g. structural violation), catch and log a warning
- Return an empty `GraphModel` rather than crashing the application startup
- Log at WARN level: "Eventus: could not extract module model — {reason}"

## Tests Required

### SpringModulithExtractorTest
Use a minimal test application defined in `test/java`:

```
io.eventus.spring.test/
├── TestApplication.java          (@SpringBootApplication)
├── order/
│   ├── OrderModule.java          (publishes OrderPlaced)
│   └── OrderPlaced.java          (record, implements DomainEvent or @DomainEvent)
└── inventory/
    └── InventoryModule.java      (listens to OrderPlaced via @ApplicationModuleListener)
```

Assert:
- `graphModel.modules()` contains "order" and "inventory"
- `graphModel.events()` contains an event named "OrderPlaced"
- `graphModel.edges()` contains a PUBLISHES edge from order and a LISTENS_TO edge to inventory

## Done When
`mvn test -pl eventus-spring` passes. The test proves the extractor reads
a real Spring Modulith module model correctly.
