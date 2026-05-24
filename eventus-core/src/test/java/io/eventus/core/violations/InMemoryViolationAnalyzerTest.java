package io.eventus.core.violations;

import io.eventus.core.memory.InMemoryGraph;
import io.eventus.core.memory.InMemoryGraphWriter;
import io.eventus.core.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryViolationAnalyzerTest {

    @Test
    void detectsUnusedEvents() {
        var writer = InMemoryGraph.writer();
        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "Order", 2, 0, ModuleStatus.HEALTHY));
        model.addEvent(new EventNode("OrderPlaced", "OrderPlaced", "order"));
        model.addEdge(new EventEdge("e1", "OrderPlaced", "order", null, EdgeType.PUBLISHES));
        // No LISTENS_TO edges → unused event
        writer.write(model);

        var analyzer = new InMemoryViolationAnalyzer(InMemoryGraph.reader(writer));
        var violations = analyzer.analyze();

        assertThat(violations).anyMatch(v -> v.type() == ViolationType.UNUSED_EVENT);
        assertThat(violations.stream().filter(v -> v.type() == ViolationType.UNUSED_EVENT))
                .allMatch(v -> v.severity() == ViolationSeverity.INFO);
    }

    @Test
    void detectsCircularEventDependencies() {
        var writer = InMemoryGraph.writer();
        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "Order", 2, 0, ModuleStatus.HEALTHY));
        model.addModule(new ModuleNode("inventory", "Inventory", 2, 0, ModuleStatus.HEALTHY));
        // A → B → A cycle: OrderPlaced → inventory, StockReserved → order
        model.addEvent(new EventNode("OrderPlaced", "OrderPlaced", "order"));
        model.addEvent(new EventNode("StockReserved", "StockReserved", "inventory"));
        model.addEdge(new EventEdge("e1", "OrderPlaced", "order", null, EdgeType.PUBLISHES));
        model.addEdge(new EventEdge("e2", "OrderPlaced", null, "inventory", EdgeType.LISTENS_TO));
        model.addEdge(new EventEdge("e3", "StockReserved", "inventory", null, EdgeType.PUBLISHES));
        model.addEdge(new EventEdge("e4", "StockReserved", null, "order", EdgeType.LISTENS_TO));
        writer.write(model);

        var analyzer = new InMemoryViolationAnalyzer(InMemoryGraph.reader(writer));
        var violations = analyzer.analyze();

        assertThat(violations).anyMatch(v -> v.type() == ViolationType.CIRCULAR_EVENT_DEPENDENCY);
    }

    @Test
    void detectsHiddenCoupling() {
        var writer = InMemoryGraph.writer();
        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "Order", 2, 0, ModuleStatus.HEALTHY));
        model.addModule(new ModuleNode("notification", "Notification", 1, 0, ModuleStatus.HEALTHY));
        model.addEvent(new EventNode("OrderPlaced", "OrderPlaced", "order"));
        model.addEdge(new EventEdge("e1", "OrderPlaced", "order", null, EdgeType.PUBLISHES));
        // notification listens but has no DEPENDS_ON edge
        model.addEdge(new EventEdge("e2", "OrderPlaced", null, "notification", EdgeType.LISTENS_TO));
        writer.write(model);

        var analyzer = new InMemoryViolationAnalyzer(InMemoryGraph.reader(writer));
        var violations = analyzer.analyze();

        assertThat(violations).anyMatch(v -> v.type() == ViolationType.HIDDEN_COUPLING);
        assertThat(violations.stream().filter(v -> v.type() == ViolationType.HIDDEN_COUPLING))
                .allMatch(v -> v.severity() == ViolationSeverity.WARNING);
    }

    @Test
    void detectsStalePublications() {
        var writer = InMemoryGraph.writer();
        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "Order", 2, 0, ModuleStatus.HEALTHY));
        // Stale publication older than 2 hours
        model.addPublication(new PublicationRecord(
                "pub1", "OrderPlaced", "InventoryService",
                "order", PublicationStatus.STALE,
                Instant.now().minus(java.time.Duration.ofHours(3))
        ));
        writer.write(model);

        var analyzer = new InMemoryViolationAnalyzer(InMemoryGraph.reader(writer));
        var violations = analyzer.analyze();

        assertThat(violations).anyMatch(v -> v.type() == ViolationType.STALE_PUBLICATION);
    }

    @Test
    void cacheReturnsSameInstanceOnSecondCall() {
        var writer = InMemoryGraph.writer();
        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "Order", 2, 0, ModuleStatus.HEALTHY));
        model.addEvent(new EventNode("OrderPlaced", "OrderPlaced", "order"));
        model.addEdge(new EventEdge("e1", "OrderPlaced", "order", null, EdgeType.PUBLISHES));
        writer.write(model);

        var analyzer = new InMemoryViolationAnalyzer(InMemoryGraph.reader(writer));
        List<Violation> first = analyzer.analyze();
        List<Violation> second = analyzer.analyze();

        assertThat(first).isSameAs(second);
    }
}
