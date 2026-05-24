package io.eventus.core.memory;

import io.eventus.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryGraphWriterTest {

    private InMemoryGraphWriter writer;
    private InMemoryGraphReader reader;

    @BeforeEach
    void setUp() {
        writer = InMemoryGraph.writer();
        reader = InMemoryGraph.reader(writer);
    }

    @Test
    void writePopulatesAllCollections() {
        var model = twoModuleModel();
        writer.write(model);

        assertThat(reader.getModules()).hasSize(2);
        assertThat(reader.getEvents()).hasSize(1);
        assertThat(reader.getEdges()).hasSize(2);
    }

    @Test
    void writeReplacesExistingData() {
        writer.write(twoModuleModel());

        var newModel = new GraphModel();
        newModel.addModule(new ModuleNode("shipping", "Shipping", 1, 0, ModuleStatus.HEALTHY));
        writer.write(newModel);

        assertThat(reader.getModules()).hasSize(1);
        assertThat(reader.getModules().get(0).id()).isEqualTo("shipping");
        assertThat(reader.getEvents()).isEmpty();
        assertThat(reader.getEdges()).isEmpty();
    }

    @Test
    void clearEmptiesAllCollections() {
        writer.write(twoModuleModel());
        writer.clear();

        assertThat(reader.getModules()).isEmpty();
        assertThat(reader.getEvents()).isEmpty();
        assertThat(reader.getEdges()).isEmpty();
        assertThat(reader.getPublications()).isEmpty();
    }

    private GraphModel twoModuleModel() {
        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "Order", 3, 1, ModuleStatus.HEALTHY));
        model.addModule(new ModuleNode("inventory", "Inventory", 2, 0, ModuleStatus.HEALTHY));
        model.addEvent(new EventNode("com.example.OrderPlaced", "OrderPlaced", "order"));
        model.addEdge(new EventEdge("e1", "com.example.OrderPlaced", "order", null, EdgeType.PUBLISHES));
        model.addEdge(new EventEdge("e2", "com.example.OrderPlaced", null, "inventory", EdgeType.LISTENS_TO));
        return model;
    }
}
