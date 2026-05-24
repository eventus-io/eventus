package io.eventus.mcp;

import io.eventus.core.model.*;
import io.eventus.core.memory.InMemoryGraph;
import io.eventus.core.memory.InMemoryGraphWriter;
import io.eventus.core.memory.InMemoryGraphReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EventusGraphToolsTest {

    private EventusGraphTools tools;

    @BeforeEach
    void setUp() {
        InMemoryGraphWriter writer = InMemoryGraph.writer();
        InMemoryGraphReader reader = InMemoryGraph.reader(writer);

        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "order", 4, 1, ModuleStatus.HEALTHY));
        model.addModule(new ModuleNode("inventory", "inventory", 3, 0, ModuleStatus.WARNING));
        model.addEvent(new EventNode("io.example.OrderPlaced", "OrderPlaced", "order"));
        model.addEdge(new EventEdge("e1", "io.example.OrderPlaced", "order", null, EdgeType.PUBLISHES));
        model.addEdge(new EventEdge("e2", "io.example.OrderPlaced", null, "inventory", EdgeType.LISTENS_TO));
        model.addPublication(new PublicationRecord(
                "pub1", "OrderPlaced", "InventoryModule::onOrderPlaced",
                "inventory", PublicationStatus.INCOMPLETE, Instant.now()));
        writer.write(model);

        tools = new EventusGraphTools(reader);
    }

    @Test
    void getModulesContainsBothModules() {
        String result = tools.getModules();
        assertThat(result).contains("\"order\"");
        assertThat(result).contains("\"inventory\"");
        assertThat(result).contains("\"HEALTHY\"");
        assertThat(result).contains("\"WARNING\"");
    }

    @Test
    void getEventsContainsOrderPlaced() {
        String result = tools.getEvents();
        assertThat(result).contains("OrderPlaced");
        assertThat(result).contains("order");
    }

    @Test
    void getEventConsumersReturnsInventory() {
        String result = tools.getEventConsumers("OrderPlaced");
        assertThat(result).contains("inventory");
        assertThat(result).doesNotContain("order");
    }

    @Test
    void getEventConsumersReturnsEmptyArrayForNonExistentEvent() {
        String result = tools.getEventConsumers("NonExistent");
        assertThat(result).isEqualTo("[]");
    }

    @Test
    void getIncompletePublicationsContainsIncompleteEntry() {
        String result = tools.getIncompletePublications();
        assertThat(result).contains("InventoryModule::onOrderPlaced");
        assertThat(result).contains("INCOMPLETE");
    }

    @Test
    void getModuleDependenciesForOrderContainsPublishesEdge() {
        String result = tools.getModuleDependencies("order");
        assertThat(result).contains("\"order\"");
        assertThat(result).contains("PUBLISHES");
        assertThat(result).contains("OrderPlaced");
    }

    @Test
    void getModuleDependenciesForUnknownModuleReturnsError() {
        String result = tools.getModuleDependencies("nonexistent");
        assertThat(result).contains("error");
    }
}
