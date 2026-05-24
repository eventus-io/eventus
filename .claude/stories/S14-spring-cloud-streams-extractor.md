# S14 — Spring Cloud Stream Extractor (Kafka)

## Goal
Build an `EventGraphExtractor` for Spring Cloud Stream applications using Kafka, making Eventus usable in async, cross-service event topologies.

## Acceptance Criteria
- [ ] New module `eventus-streams` created
- [ ] `SpringCloudStreamExtractor` implements `EventGraphExtractor`
- [ ] Reads bindings from Spring Cloud Stream configuration
- [ ] Creates ModuleNode per service/application
- [ ] Creates EventNode per topic/binding
- [ ] Creates PUBLISHES edges for output bindings, LISTENS_TO for input bindings
- [ ] Detects service names from `spring.application.name` property
- [ ] Handles missing bindings gracefully (logs WARN, continues)
- [ ] Integration test with embedded Kafka
- [ ] All tests green

## New Module Structure

```
eventus-streams/
├── pom.xml
└── src/
    ├── main/java/io/eventus/streams/
    │   ├── SpringCloudStreamExtractor.java
    │   ├── model/
    │   │   ├── StreamBinding.java
    │   │   └── StreamTopology.java
    │   └── config/
    │       └── KafkaBindingReader.java
    └── test/java/io/eventus/streams/
        ├── SpringCloudStreamExtractorTest.java
        └── testapp/
            ├── ProducerApp.java
            ├── ConsumerApp.java
            └── StreamTestApplication.java
```

## Dependencies

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.eventus</groupId>
    <artifactId>eventus-core</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream</artifactId>
    <version>${spring-cloud-stream.version}</version>
    <scope>provided</scope>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream-binder-kafka</artifactId>
    <scope>test</scope>
</dependency>

<!-- test: embedded Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Implementation

### Value Objects

```java
// io.eventus.streams.model/StreamBinding.java
public record StreamBinding(
    String name,              // e.g., "orders-out", "inventory-in"
    String contentType,       // e.g., "application/json"
    BindingType type,        // OUTPUT, INPUT
    String topic,            // Kafka topic name
    String group             // consumer group (null for output)
) {}

public enum BindingType { INPUT, OUTPUT }

// io.eventus.streams.model/StreamTopology.java
public record StreamTopology(
    String applicationName,
    List<StreamBinding> bindings
) {}
```

### Kafka Binding Reader

```java
// io.eventus.streams.config/KafkaBindingReader.java
public class KafkaBindingReader {
    private final ConfigurableEnvironment environment;

    public KafkaBindingReader(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    public StreamTopology readTopology(String appName) {
        List<StreamBinding> bindings = new ArrayList<>();

        // Read spring.cloud.stream.bindings.* properties
        Map<String, Object> properties = environment.getSystemProperties();
        
        properties.forEach((key, value) -> {
            if (key.startsWith("spring.cloud.stream.bindings.")) {
                String[] parts = key.split("\\.");
                if (parts.length >= 5) {
                    String bindingName = parts[4];
                    String property = parts[5];
                    
                    if ("destination".equals(property)) {
                        String destination = value.toString();
                        BindingType type = inferBindingType(bindingName);
                        String group = inferGroup(bindingName, type);
                        
                        bindings.add(new StreamBinding(
                            bindingName,
                            "application/json",
                            type,
                            destination,
                            group
                        ));
                    }
                }
            }
        });

        return new StreamTopology(appName, bindings);
    }

    private BindingType inferBindingType(String bindingName) {
        // Convention: "*-in" = INPUT, "*-out" = OUTPUT
        return bindingName.endsWith("-in") ? BindingType.INPUT : BindingType.OUTPUT;
    }

    private String inferGroup(String bindingName, BindingType type) {
        if (type == BindingType.INPUT) {
            return bindingName.replace("-in", "");
        }
        return null;
    }
}
```

### Spring Cloud Stream Extractor

```java
// io.eventus.streams/SpringCloudStreamExtractor.java
public class SpringCloudStreamExtractor implements EventGraphExtractor {
    private final KafkaBindingReader bindingReader;
    private final String applicationName;

    public SpringCloudStreamExtractor(ConfigurableEnvironment environment) {
        this.bindingReader = new KafkaBindingReader(environment);
        this.applicationName = environment.getProperty("spring.application.name", "unknown");
    }

    @Override
    public GraphModel extract() {
        GraphModel model = new GraphModel();
        
        try {
            StreamTopology topology = bindingReader.readTopology(applicationName);
            
            // Create module node for this service
            ModuleNode appModule = new ModuleNode(
                applicationName,
                applicationName,
                0,  // bean count not available from config
                0,  // aggregate count not available
                ModuleStatus.HEALTHY
            );
            model.addModule(appModule);

            // Process each binding
            for (StreamBinding binding : topology.bindings()) {
                String eventId = createEventId(binding.topic(), binding.group());
                
                EventNode event = new EventNode(
                    eventId,
                    binding.topic(),
                    binding.type() == BindingType.OUTPUT ? applicationName : "external"
                );
                model.addEvent(event);

                EdgeType edgeType = binding.type() == BindingType.OUTPUT 
                    ? EdgeType.PUBLISHES 
                    : EdgeType.LISTENS_TO;

                String targetModule = binding.type() == BindingType.OUTPUT 
                    ? binding.topic() + "-consumers"
                    : applicationName;

                EventEdge edge = new EventEdge(
                    UUID.randomUUID().toString(),
                    eventId,
                    applicationName,
                    targetModule,
                    edgeType
                );
                model.addEdge(edge);
            }

        } catch (Exception e) {
            log.warn("Failed to extract Spring Cloud Stream topology: {}", e.getMessage());
        }

        return model;
    }

    private String createEventId(String topic, String group) {
        if (group != null) {
            return topic + ":" + group;
        }
        return topic;
    }

    @Override
    public String name() {
        return "SpringCloudStreamExtractor";
    }
}
```

## Integration Test

```java
// src/test/java/io/eventus/streams/SpringCloudStreamExtractorTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092" })
@TestPropertySource(properties = {
    "spring.application.name=order-service",
    "spring.cloud.stream.bindings.orders-out.destination=orders-topic",
    "spring.cloud.stream.bindings.inventory-in.destination=inventory-topic",
    "spring.cloud.stream.bindings.inventory-in.group=order-service"
})
class SpringCloudStreamExtractorTest {
    
    @Autowired ConfigurableEnvironment environment;
    private SpringCloudStreamExtractor extractor;

    @BeforeEach
    void setup() {
        extractor = new SpringCloudStreamExtractor(environment);
    }

    @Test
    void extractsServiceAsModuleNode() {
        GraphModel model = extractor.extract();
        
        assertThat(model.modules())
            .hasSize(1)
            .extracting(ModuleNode::name)
            .contains("order-service");
    }

    @Test
    void extractsBindingsAsEvents() {
        GraphModel model = extractor.extract();
        
        assertThat(model.events())
            .hasSize(2)
            .extracting(EventNode::name)
            .containsExactlyInAnyOrder("orders-topic", "inventory-topic");
    }

    @Test
    void createsPublishesEdgeForOutputBinding() {
        GraphModel model = extractor.extract();
        
        assertThat(model.edges())
            .anyMatch(e -> e.edgeType() == EdgeType.PUBLISHES && 
                          e.fromModuleId().equals("order-service") &&
                          e.toModuleId().equals("orders-topic-consumers"));
    }

    @Test
    void createsListensToEdgeForInputBinding() {
        GraphModel model = extractor.extract();
        
        assertThat(model.edges())
            .anyMatch(e -> e.edgeType() == EdgeType.LISTENS_TO && 
                          e.fromModuleId().equals("order-service"));
    }

    @Test
    void returnsEmptyModelOnExtractionFailure() {
        SpringCloudStreamExtractor failingExtractor = 
            new SpringCloudStreamExtractor(new StandardEnvironment());
        
        GraphModel model = failingExtractor.extract();
        assertThat(model.modules()).isEmpty();
    }
}
```

## Maven Configuration (parent pom.xml)

Add to `<dependencyManagement>`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-dependencies</artifactId>
    <version>${spring-cloud.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

Add property:
```xml
<spring-cloud.version>2024.0.0</spring-cloud.version>
<spring-cloud-stream.version>4.0.5</spring-cloud-stream.version>
```

## Documentation

Create `eventus-streams/README.md`:

```markdown
# Eventus Streams

Spring Cloud Stream integration for Eventus. Extracts event topology from Kafka-based applications.

## Usage

Add to your Spring Cloud Stream application:

```xml
<dependency>
    <groupId>io.eventus</groupId>
    <artifactId>eventus-streams</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

Ensure Eventus Spring is also on classpath (for actuator endpoints).

## How It Works

Reads Spring Cloud Stream bindings from configuration and creates:
- **ModuleNode**: Your service (`spring.application.name`)
- **EventNode**: Per Kafka topic
- **PUBLISHES edges**: For output bindings
- **LISTENS_TO edges**: For input bindings

Visit `/actuator/eventus-modules` to see the topology.
```

## Done When
- New module `eventus-streams` builds successfully
- All integration tests pass with embedded Kafka
- Extractor correctly reads bindings and creates graph model
- `mvn verify -pl eventus-streams` succeeds
- Documentation complete with usage examples
