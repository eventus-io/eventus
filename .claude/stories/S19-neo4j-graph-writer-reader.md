# S19 — Neo4j Backend: GraphWriter + GraphReader Implementation

## Goal
Implement `Neo4jGraphWriter` and `Neo4jGraphReader` — the production-grade `GraphWriter`/`GraphReader` pair backed by Neo4j, replacing the in-memory backend when Neo4j is on the classpath.

## Acceptance Criteria
- [ ] `Neo4jGraphWriter` persists `ModuleNode`, `EventNode`, `EventEdge`, and `EventPublication` as Neo4j nodes/relationships
- [ ] `Neo4jGraphReader` reads back `ModuleNode`, `EventNode`, `EventEdge`, and `EventPublication` from Neo4j
- [ ] Round-trip test: write a graph, read it back, assert equality
- [ ] Idempotent writes: writing the same node twice does not create duplicates (`MERGE` semantics)
- [ ] Tests use Testcontainers Neo4j (no mocks for Neo4j itself)
- [ ] `mvn verify -pl eventus-neo4j` passes

## Neo4j Data Model

```cypher
// Nodes
(:Module {id, name, beanCount, aggregateCount, status})
(:DomainEvent {id, name, publisherModuleId})
(:Publication {id, eventType, listenerName, moduleId, status, publishedAt})

// Relationships
(:Module)-[:PUBLISHES]->(:DomainEvent)
(:DomainEvent)-[:CONSUMED_BY]->(:Module)
(:Publication)-[:FOR_EVENT]->(:DomainEvent)
(:Publication)-[:IN_MODULE]->(:Module)
```

## Implementation

```java
// io.eventus.neo4j/Neo4jGraphWriter.java
public class Neo4jGraphWriter implements GraphWriter {
    private final Neo4jClient client;

    public Neo4jGraphWriter(Neo4jClient client) {
        this.client = client;
    }

    @Override
    public void writeModule(ModuleNode module) {
        client.query("""
            MERGE (m:Module {id: $id})
            SET m.name = $name,
                m.beanCount = $beanCount,
                m.aggregateCount = $aggregateCount,
                m.status = $status
            """)
            .bind(module.id()).to("id")
            .bind(module.name()).to("name")
            .bind(module.beanCount()).to("beanCount")
            .bind(module.aggregateCount()).to("aggregateCount")
            .bind(module.status().name()).to("status")
            .run();
    }

    @Override
    public void writeEvent(EventNode event) {
        client.query("""
            MERGE (e:DomainEvent {id: $id})
            SET e.name = $name,
                e.publisherModuleId = $publisherModuleId
            """)
            .bind(event.id()).to("id")
            .bind(event.name()).to("name")
            .bind(event.publisherModuleId()).to("publisherModuleId")
            .run();
    }

    @Override
    public void writeEdge(EventEdge edge) {
        // Ensure source module and event exist, then create relationship
        client.query("""
            MATCH (m:Module {id: $fromModuleId})
            MATCH (e:DomainEvent {id: $eventId})
            MERGE (m)-[:PUBLISHES]->(e)
            """)
            .bind(edge.fromModuleId()).to("fromModuleId")
            .bind(edge.eventId()).to("eventId")
            .run();

        client.query("""
            MATCH (e:DomainEvent {id: $eventId})
            MATCH (m:Module {id: $toModuleId})
            MERGE (e)-[:CONSUMED_BY]->(m)
            """)
            .bind(edge.eventId()).to("eventId")
            .bind(edge.toModuleId()).to("toModuleId")
            .run();
    }

    @Override
    public void writePublication(EventPublication pub) {
        client.query("""
            MERGE (p:Publication {id: $id})
            SET p.eventType = $eventType,
                p.listenerName = $listenerName,
                p.moduleId = $moduleId,
                p.status = $status,
                p.publishedAt = $publishedAt
            """)
            .bind(pub.id()).to("id")
            .bind(pub.eventType()).to("eventType")
            .bind(pub.listenerName()).to("listenerName")
            .bind(pub.moduleId()).to("moduleId")
            .bind(pub.status().name()).to("status")
            .bind(pub.publishedAt()).to("publishedAt")
            .run();
    }
}
```

```java
// io.eventus.neo4j/Neo4jGraphReader.java
public class Neo4jGraphReader implements GraphReader {
    private final Neo4jClient client;

    public Neo4jGraphReader(Neo4jClient client) {
        this.client = client;
    }

    @Override
    public List<ModuleNode> getModules() {
        return client.query("MATCH (m:Module) RETURN m")
            .fetchAs(ModuleNode.class)
            .mappedBy((typeSystem, record) -> {
                var m = record.get("m").asNode();
                return new ModuleNode(
                    m.get("id").asString(),
                    m.get("name").asString(),
                    (int) m.get("beanCount").asLong(),
                    (int) m.get("aggregateCount").asLong(),
                    ModuleStatus.valueOf(m.get("status").asString())
                );
            })
            .all().stream().toList();
    }

    @Override
    public List<EventNode> getEvents() {
        return client.query("MATCH (e:DomainEvent) RETURN e")
            .fetchAs(EventNode.class)
            .mappedBy((typeSystem, record) -> {
                var e = record.get("e").asNode();
                return new EventNode(
                    e.get("id").asString(),
                    e.get("name").asString(),
                    e.get("publisherModuleId").asString()
                );
            })
            .all().stream().toList();
    }

    @Override
    public List<EventEdge> getEdges() {
        return client.query("""
            MATCH (from:Module)-[:PUBLISHES]->(e:DomainEvent)-[:CONSUMED_BY]->(to:Module)
            RETURN from.id AS fromModuleId, e.id AS eventId, to.id AS toModuleId
            """)
            .fetchAs(EventEdge.class)
            .mappedBy((typeSystem, record) -> new EventEdge(
                record.get("fromModuleId").asString() + "_" + record.get("eventId").asString() + "_" + record.get("toModuleId").asString(),
                record.get("fromModuleId").asString(),
                record.get("eventId").asString(),
                record.get("toModuleId").asString()
            ))
            .all().stream().toList();
    }

    @Override
    public List<EventPublication> getPublications() {
        return client.query("MATCH (p:Publication) RETURN p")
            .fetchAs(EventPublication.class)
            .mappedBy((typeSystem, record) -> {
                var p = record.get("p").asNode();
                return new EventPublication(
                    p.get("id").asString(),
                    p.get("eventType").asString(),
                    p.get("listenerName").asString(),
                    p.get("moduleId").asString(),
                    PublicationStatus.valueOf(p.get("status").asString()),
                    p.get("publishedAt").asLong()
                );
            })
            .all().stream().toList();
    }
}
```

## Tests Required

```java
// Uses Testcontainers — requires Docker
@Testcontainers
class Neo4jGraphWriterReaderTest {

    @Container
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5")
        .withoutAuthentication();

    private Neo4jGraphWriter writer;
    private Neo4jGraphReader reader;

    @BeforeEach
    void setUp() {
        // Build Neo4jClient from container bolt URI
        Neo4jClient client = Neo4jClient.create(
            GraphDatabase.driver(neo4j.getBoltUrl())
        );
        writer = new Neo4jGraphWriter(client);
        reader = new Neo4jGraphReader(client);
    }

    @Test
    void roundTrip_module() {
        var module = new ModuleNode("order", "order", 3, 1, ModuleStatus.HEALTHY);
        writer.writeModule(module);
        assertThat(reader.getModules()).containsExactly(module);
    }

    @Test
    void roundTrip_event() {
        var event = new EventNode("evt-1", "OrderPlaced", "order");
        writer.writeEvent(event);
        assertThat(reader.getEvents()).containsExactly(event);
    }

    @Test
    void idempotentWrite_doesNotDuplicate() {
        var module = new ModuleNode("order", "order", 3, 1, ModuleStatus.HEALTHY);
        writer.writeModule(module);
        writer.writeModule(module);
        assertThat(reader.getModules()).hasSize(1);
    }

    @Test
    void roundTrip_edge() {
        var from = new ModuleNode("order", "order", 1, 0, ModuleStatus.HEALTHY);
        var to = new ModuleNode("inventory", "inventory", 1, 0, ModuleStatus.HEALTHY);
        var event = new EventNode("evt-1", "OrderPlaced", "order");
        writer.writeModule(from);
        writer.writeModule(to);
        writer.writeEvent(event);
        writer.writeEdge(new EventEdge("e1", "order", "evt-1", "inventory"));
        assertThat(reader.getEdges()).hasSize(1);
    }
}
```

## Done When
- All tests pass with real Neo4j via Testcontainers
- `MERGE` semantics confirmed (no duplicate nodes on repeated writes)
- `mvn verify -pl eventus-neo4j` green
