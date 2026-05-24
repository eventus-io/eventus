package io.eventus.core.impact;

import io.eventus.core.memory.InMemoryGraph;
import io.eventus.core.memory.InMemoryGraphWriter;
import io.eventus.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryImpactAnalyzerTest {

    private InMemoryImpactAnalyzer analyzer;

    @BeforeEach
    void setup() {
        var writer = InMemoryGraph.writer();
        var reader = InMemoryGraph.reader(writer);

        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "Order", 3, 0, ModuleStatus.HEALTHY));
        model.addModule(new ModuleNode("inventory", "Inventory", 2, 0, ModuleStatus.HEALTHY));
        model.addModule(new ModuleNode("notification", "Notification", 1, 0, ModuleStatus.HEALTHY));
        model.addEvent(new EventNode("OrderPlaced", "OrderPlaced", "order"));
        model.addEdge(new EventEdge("e1", "OrderPlaced", "order", null, EdgeType.PUBLISHES));
        model.addEdge(new EventEdge("e2", "OrderPlaced", null, "inventory", EdgeType.LISTENS_TO));
        model.addEdge(new EventEdge("e3", "OrderPlaced", null, "notification", EdgeType.LISTENS_TO));
        writer.write(model);

        analyzer = new InMemoryImpactAnalyzer(reader);
    }

    @Test
    void analyzeEventImpactReturnsAllListeners() {
        var response = analyzer.analyzeEventImpact("OrderPlaced");

        assertThat(response.affectedModules()).hasSize(2);
        assertThat(response.affectedModules().stream().map(AffectedModule::moduleId))
                .containsExactlyInAnyOrder("inventory", "notification");
    }

    @Test
    void analyzeEventImpactSetsDirectListenersCount() {
        var response = analyzer.analyzeEventImpact("OrderPlaced");

        assertThat(response.directListeners()).isEqualTo(2);
        assertThat(response.eventId()).isEqualTo("OrderPlaced");
        assertThat(response.publisherModuleId()).isEqualTo("order");
    }

    @Test
    void analyzeModuleImpactReturnsPublishedEvents() {
        var response = analyzer.analyzeModuleImpact("order");

        assertThat(response.publishedEvents()).hasSize(1);
        assertThat(response.publishedEvents().get(0).eventName()).isEqualTo("OrderPlaced");
    }

    @Test
    void analyzeModuleImpactCountsDownstreamModules() {
        var response = analyzer.analyzeModuleImpact("order");

        assertThat(response.totalAffectedModules()).isEqualTo(2);
        assertThat(response.downstreamModules().stream().map(DownstreamModule::moduleId))
                .containsExactlyInAnyOrder("inventory", "notification");
    }

    @Test
    void throwsEventNotFoundExceptionForUnknownEvent() {
        assertThatThrownBy(() -> analyzer.analyzeEventImpact("NonExistent"))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("NonExistent");
    }

    @Test
    void throwsModuleNotFoundExceptionForUnknownModule() {
        assertThatThrownBy(() -> analyzer.analyzeModuleImpact("unknown"))
                .isInstanceOf(ModuleNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
