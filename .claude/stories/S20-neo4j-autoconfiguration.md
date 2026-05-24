# S20 — Neo4j Backend: Auto-Configuration + Schema Initialisation

## Goal
Complete `EventusNeo4jAutoConfiguration` so it registers `Neo4jGraphWriter` and `Neo4jGraphReader` as primary beans when Neo4j is on the classpath, creates the required indexes and constraints on startup, and allows the MCP server to expose a Cypher query tool.

## Acceptance Criteria
- [ ] `EventusNeo4jAutoConfiguration` registers `Neo4jGraphWriter` as `GraphWriter` bean with `@Primary`
- [ ] `EventusNeo4jAutoConfiguration` registers `Neo4jGraphReader` as `GraphReader` bean with `@Primary`
- [ ] Both beans are `@ConditionalOnMissingBean` so users can override
- [ ] Schema initialiser creates uniqueness constraints and indexes on startup
- [ ] Integration test: Spring Boot app with Neo4j Testcontainers starts and the actuator endpoints return data persisted in Neo4j
- [ ] If `eventus.mcp.cypher.enabled=true`, a `runCypherQuery` MCP tool is registered
- [ ] `mvn verify` passes

## Auto-Configuration

```java
// io.eventus.neo4j.autoconfigure/EventusNeo4jAutoConfiguration.java
@Configuration
@ConditionalOnClass(name = "org.springframework.data.neo4j.core.Neo4jClient")
@ConditionalOnProperty(prefix = "eventus.neo4j", name = "enabled", matchIfMissing = true)
@AutoConfiguration(after = Neo4jDataAutoConfiguration.class)
public class EventusNeo4jAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(GraphWriter.class)
    public GraphWriter neo4jGraphWriter(Neo4jClient client) {
        return new Neo4jGraphWriter(client);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(GraphReader.class)
    public GraphReader neo4jGraphReader(Neo4jClient client) {
        return new Neo4jGraphReader(client);
    }

    @Bean
    public EventusNeo4jSchemaInitialiser neo4jSchemaInitialiser(Neo4jClient client) {
        return new EventusNeo4jSchemaInitialiser(client);
    }
}
```

## Schema Initialiser

```java
// io.eventus.neo4j/EventusNeo4jSchemaInitialiser.java
public class EventusNeo4jSchemaInitialiser implements ApplicationRunner {
    private final Neo4jClient client;

    public EventusNeo4jSchemaInitialiser(Neo4jClient client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) {
        client.query("CREATE CONSTRAINT module_id IF NOT EXISTS FOR (m:Module) REQUIRE m.id IS UNIQUE").run();
        client.query("CREATE CONSTRAINT event_id IF NOT EXISTS FOR (e:DomainEvent) REQUIRE e.id IS UNIQUE").run();
        client.query("CREATE CONSTRAINT publication_id IF NOT EXISTS FOR (p:Publication) REQUIRE p.id IS UNIQUE").run();
        client.query("CREATE INDEX module_status IF NOT EXISTS FOR (m:Module) ON (m.status)").run();
        client.query("CREATE INDEX publication_status IF NOT EXISTS FOR (p:Publication) ON (p.status)").run();
    }
}
```

## MCP Cypher Tool (conditional)

Add to `eventus-mcp` when `eventus.mcp.cypher.enabled=true` and Neo4j is present:

```java
// io.eventus.mcp/EventusCypherTool.java
@Component
@ConditionalOnProperty(prefix = "eventus.mcp.cypher", name = "enabled")
@ConditionalOnClass(name = "org.springframework.data.neo4j.core.Neo4jClient")
public class EventusCypherTool {
    private final Neo4jClient client;

    @Tool(description = "Run a read-only Cypher query against the Eventus Neo4j graph. Use MATCH queries only. Returns results as JSON array.")
    public String runCypherQuery(
        @ToolParam(description = "A read-only Cypher MATCH query") String cypher
    ) {
        if (!cypher.trim().toUpperCase().startsWith("MATCH")) {
            return "{\"error\": \"Only MATCH queries are permitted\"}";
        }
        var results = client.query(cypher).fetch().all();
        return results.toString();
    }
}
```

## Integration Test

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class Neo4jIntegrationTest {

    @Container
    @ServiceConnection
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5")
        .withoutAuthentication();

    @Autowired MockMvc mockMvc;
    @Autowired GraphWriter writer;

    @Test
    void graphWriterIsNeo4jBacked() {
        assertThat(writer).isInstanceOf(Neo4jGraphWriter.class);
    }

    @Test
    void modulesEndpointReturnsPersisted() throws Exception {
        writer.writeModule(new ModuleNode("order", "order", 2, 1, ModuleStatus.HEALTHY));

        mockMvc.perform(get("/actuator/eventus-modules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("order"));
    }

    @Test
    void dataPersistedAcrossContextReloads() {
        // Write via writer, verify via reader, restart context (or reload bean), read again
        writer.writeModule(new ModuleNode("payments", "payments", 1, 0, ModuleStatus.HEALTHY));
        assertThat(reader.getModules()).extracting(ModuleNode::id).contains("payments");
    }
}
```

## Configuration Properties

```properties
# Optional — Neo4j auto-config is on by default when spring-data-neo4j is present
eventus.neo4j.enabled=true

# Opt in to the Cypher MCP tool (use with caution — exposes graph to LLMs)
eventus.mcp.cypher.enabled=false
```

## Done When
- Spring Boot app starts with Neo4j Testcontainers and `GraphWriter` bean is `Neo4jGraphWriter`
- Constraints and indexes created on startup without errors
- Actuator endpoints return data read from Neo4j
- `eventus.mcp.cypher.enabled=false` by default (safe opt-in)
- `mvn verify` green
