package io.eventus.core.drift;

import io.eventus.core.memory.InMemoryGraph;
import io.eventus.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryDriftAnalyzerTest {

    private InMemoryDriftAnalyzer analyzer;

    @BeforeEach
    void setup() {
        var writer = InMemoryGraph.writer();
        var model = new GraphModel();
        // Current state: only inventory module
        model.addModule(new ModuleNode("inventory", "Inventory", 2, 0, ModuleStatus.HEALTHY));
        model.addEvent(new EventNode("OrderPlaced", "OrderPlaced", "order"));
        model.addEdge(new EventEdge("e2", "OrderPlaced", null, "inventory", EdgeType.LISTENS_TO));
        writer.write(model);

        // Baseline: had both order AND inventory, plus OrderPlaced event
        var baseline = new BaselineSnapshot(
                List.of(
                        new ModuleNode("order", "Order", 3, 0, ModuleStatus.HEALTHY),
                        new ModuleNode("inventory", "Inventory", 2, 0, ModuleStatus.HEALTHY)
                ),
                List.of(new EventNode("OrderPlaced", "OrderPlaced", "order")),
                List.of(
                        new EventEdge("e1", "OrderPlaced", "order", null, EdgeType.PUBLISHES),
                        new EventEdge("e2", "OrderPlaced", null, "inventory", EdgeType.LISTENS_TO)
                ),
                System.currentTimeMillis() - 60_000
        );

        var baselineManager = new StubBaselineManager(baseline);
        analyzer = new InMemoryDriftAnalyzer(InMemoryGraph.reader(writer), baselineManager);
    }

    @Test
    void detectsRemovedModule() {
        var report = analyzer.analyzeDrift();

        assertThat(report.drifts()).anyMatch(d -> d.type() == DriftType.MODULE_REMOVED);
    }

    @Test
    void removedModuleWithListenersDependingOnItIsBREAKING() {
        var report = analyzer.analyzeDrift();

        assertThat(report.drifts())
                .filteredOn(d -> d.type() == DriftType.MODULE_REMOVED)
                .anyMatch(d -> d.severity() == DriftSeverity.BREAKING);
    }

    @Test
    void detectsAddedEvent() {
        var writer2 = InMemoryGraph.writer();
        var model2 = new GraphModel();
        model2.addModule(new ModuleNode("order", "Order", 3, 0, ModuleStatus.HEALTHY));
        model2.addModule(new ModuleNode("inventory", "Inventory", 2, 0, ModuleStatus.HEALTHY));
        model2.addEvent(new EventNode("OrderPlaced", "OrderPlaced", "order"));
        model2.addEvent(new EventNode("OrderCancelled", "OrderCancelled", "order")); // new event
        model2.addEdge(new EventEdge("e1", "OrderPlaced", "order", null, EdgeType.PUBLISHES));
        model2.addEdge(new EventEdge("e2", "OrderPlaced", null, "inventory", EdgeType.LISTENS_TO));
        writer2.write(model2);

        var baseline2 = new BaselineSnapshot(
                List.of(
                        new ModuleNode("order", "Order", 3, 0, ModuleStatus.HEALTHY),
                        new ModuleNode("inventory", "Inventory", 2, 0, ModuleStatus.HEALTHY)
                ),
                List.of(new EventNode("OrderPlaced", "OrderPlaced", "order")),
                List.of(
                        new EventEdge("e1", "OrderPlaced", "order", null, EdgeType.PUBLISHES),
                        new EventEdge("e2", "OrderPlaced", null, "inventory", EdgeType.LISTENS_TO)
                ),
                System.currentTimeMillis() - 60_000
        );

        var analyzer2 = new InMemoryDriftAnalyzer(InMemoryGraph.reader(writer2), new StubBaselineManager(baseline2));
        var report = analyzer2.analyzeDrift();

        assertThat(report.drifts())
                .anyMatch(d -> d.type() == DriftType.EVENT_ADDED && "OrderCancelled".equals(d.affectedItemName()));
    }

    @Test
    void returnsEmptyReportWhenNoBaseline() {
        var writer = InMemoryGraph.writer();
        var report = new InMemoryDriftAnalyzer(InMemoryGraph.reader(writer), new StubBaselineManager(null))
                .analyzeDrift();

        assertThat(report.drifts()).isEmpty();
        assertThat(report.totalDrifts()).isZero();
    }

    @Test
    void breachingCountIncludesOnlyBreakingDrifts() {
        var report = analyzer.analyzeDrift();

        long breaking = report.drifts().stream()
                .filter(d -> d.severity() == DriftSeverity.BREAKING)
                .count();
        assertThat(report.breachingCount()).isEqualTo((int) breaking);
    }

    private record StubBaselineManager(BaselineSnapshot snapshot) implements BaselineManager {
        @Override public BaselineSnapshot loadBaseline() { return snapshot; }
        @Override public void saveBaseline(io.eventus.core.model.GraphModel m) {}
        @Override public boolean hasBaseline() { return snapshot != null; }
    }
}
