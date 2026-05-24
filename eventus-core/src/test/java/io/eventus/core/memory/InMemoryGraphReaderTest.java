package io.eventus.core.memory;

import io.eventus.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryGraphReaderTest {

    private InMemoryGraphWriter writer;
    private InMemoryGraphReader reader;

    @BeforeEach
    void setUp() {
        writer = InMemoryGraph.writer();
        reader = InMemoryGraph.reader(writer);

        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "Order", 3, 1, ModuleStatus.HEALTHY));
        model.addModule(new ModuleNode("inventory", "Inventory", 2, 0, ModuleStatus.HEALTHY));
        model.addEvent(new EventNode("com.example.OrderPlaced", "OrderPlaced", "order"));
        model.addEdge(new EventEdge("e1", "com.example.OrderPlaced", "order", null, EdgeType.PUBLISHES));
        model.addEdge(new EventEdge("e2", "com.example.OrderPlaced", null, "inventory", EdgeType.LISTENS_TO));
        model.addEdge(new EventEdge("e3", "com.example.ShipmentCreated", "inventory", null, EdgeType.PUBLISHES));
        model.addPublication(new PublicationRecord("p1", "com.example.OrderPlaced",
                "InventoryService.on", "inventory", PublicationStatus.INCOMPLETE, Instant.now()));
        model.addPublication(new PublicationRecord("p2", "com.example.OrderPlaced",
                "NotificationService.on", "notification", PublicationStatus.COMPLETED, Instant.now()));
        model.addPublication(new PublicationRecord("p3", "com.example.OrderPlaced",
                "AuditService.on", "audit", PublicationStatus.STALE, Instant.now()));
        writer.write(model);
    }

    @Test
    void getEdgesForEventReturnsOnlyMatchingEdges() {
        var edges = reader.getEdgesForEvent("com.example.OrderPlaced");
        assertThat(edges).hasSize(2);
        assertThat(edges).allMatch(e -> e.eventId().equals("com.example.OrderPlaced"));
    }

    @Test
    void getIncompletePublicationsExcludesCompleted() {
        var incomplete = reader.getIncompletePublications();
        assertThat(incomplete).hasSize(2);
        assertThat(incomplete).noneMatch(p -> p.status() == PublicationStatus.COMPLETED);
        assertThat(incomplete).anyMatch(p -> p.status() == PublicationStatus.INCOMPLETE);
        assertThat(incomplete).anyMatch(p -> p.status() == PublicationStatus.STALE);
    }

    @Test
    void modulesListIsUnmodifiable() {
        assertThatThrownBy(() -> reader.getModules().add(
                new ModuleNode("x", "X", 0, 0, ModuleStatus.HEALTHY)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void edgesListIsUnmodifiable() {
        assertThatThrownBy(() -> reader.getEdges().add(
                new EventEdge("x", "y", "a", "b", EdgeType.PUBLISHES)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void publicationsListIsUnmodifiable() {
        assertThatThrownBy(() -> reader.getPublications().add(
                new PublicationRecord("x", "type", "listener", "m1", PublicationStatus.COMPLETED, Instant.now())))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
