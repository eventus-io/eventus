package io.eventus.neo4j;

import io.eventus.core.model.EdgeType;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.GraphModel;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.ModuleStatus;
import io.eventus.core.model.PublicationRecord;
import io.eventus.core.model.PublicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class Neo4jGraphWriterReaderTest {

    @Container
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5")
            .withoutAuthentication();

    private Neo4jGraphWriter writer;
    private Neo4jGraphReader reader;

    @BeforeEach
    void setUp() {
        var driver = GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.none());
        var client = Neo4jClient.create(driver);
        writer = new Neo4jGraphWriter(client);
        reader = new Neo4jGraphReader(client);
        writer.clear();
    }

    @Test
    void roundTrip_modules() {
        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "order", 3, 1, ModuleStatus.HEALTHY));
        model.addModule(new ModuleNode("inventory", "inventory", 2, 0, ModuleStatus.WARNING));
        writer.write(model);

        var modules = reader.getModules();
        assertThat(modules).hasSize(2);
        assertThat(modules).extracting(ModuleNode::id).containsExactlyInAnyOrder("order", "inventory");
        assertThat(modules).extracting(ModuleNode::status).containsExactlyInAnyOrder(ModuleStatus.HEALTHY, ModuleStatus.WARNING);
    }

    @Test
    void roundTrip_events() {
        var model = new GraphModel();
        model.addEvent(new EventNode("evt-1", "OrderPlaced", "order"));
        writer.write(model);

        var events = reader.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).name()).isEqualTo("OrderPlaced");
        assertThat(events.get(0).publisherModuleId()).isEqualTo("order");
    }

    @Test
    void roundTrip_edges() {
        var model = new GraphModel();
        model.addEdge(new EventEdge("e1", "evt-1", "order", "inventory", EdgeType.PUBLISHES));
        writer.write(model);

        var edges = reader.getEdges();
        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).fromModuleId()).isEqualTo("order");
        assertThat(edges.get(0).toModuleId()).isEqualTo("inventory");
        assertThat(edges.get(0).edgeType()).isEqualTo(EdgeType.PUBLISHES);
    }

    @Test
    void roundTrip_publications() {
        var now = Instant.now();
        var model = new GraphModel();
        model.addPublication(new PublicationRecord("pub-1", "OrderPlaced", "InventoryService.onOrderPlaced", "inventory", PublicationStatus.COMPLETED, now));
        writer.write(model);

        var pubs = reader.getPublications();
        assertThat(pubs).hasSize(1);
        assertThat(pubs.get(0).status()).isEqualTo(PublicationStatus.COMPLETED);
        assertThat(pubs.get(0).publishedAt().toEpochMilli()).isEqualTo(now.toEpochMilli());
    }

    @Test
    void getIncompletePublications_filtersCorrectly() {
        var model = new GraphModel();
        model.addPublication(new PublicationRecord("p1", "E1", "L1", "m1", PublicationStatus.COMPLETED, Instant.now()));
        model.addPublication(new PublicationRecord("p2", "E2", "L2", "m2", PublicationStatus.INCOMPLETE, Instant.now()));
        model.addPublication(new PublicationRecord("p3", "E3", "L3", "m3", PublicationStatus.STALE, Instant.now()));
        writer.write(model);

        var incomplete = reader.getIncompletePublications();
        assertThat(incomplete).hasSize(2);
        assertThat(incomplete).extracting(PublicationRecord::id).containsExactlyInAnyOrder("p2", "p3");
    }

    @Test
    void write_clearsAndReplacesExistingData() {
        var model1 = new GraphModel();
        model1.addModule(new ModuleNode("old-module", "old", 1, 0, ModuleStatus.HEALTHY));
        writer.write(model1);

        var model2 = new GraphModel();
        model2.addModule(new ModuleNode("new-module", "new", 1, 0, ModuleStatus.HEALTHY));
        writer.write(model2);

        var modules = reader.getModules();
        assertThat(modules).hasSize(1);
        assertThat(modules.get(0).id()).isEqualTo("new-module");
    }

    @Test
    void getEdgesForEvent_returnsOnlyMatchingEdges() {
        var model = new GraphModel();
        model.addEdge(new EventEdge("e1", "evt-1", "order", "inventory", EdgeType.PUBLISHES));
        model.addEdge(new EventEdge("e2", "evt-2", "order", "payment", EdgeType.PUBLISHES));
        writer.write(model);

        var edges = reader.getEdgesForEvent("evt-1");
        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).id()).isEqualTo("e1");
    }
}
