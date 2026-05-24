package io.eventus.core;

import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventEdge.EdgeType;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.ModuleNode.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryGraphTest {

    private InMemoryGraphWriter writer;
    private InMemoryGraphReader reader;

    @BeforeEach
    void setUp() {
        writer = new InMemoryGraphWriter();
        reader = new InMemoryGraphReader(writer);
    }

    @Test
    void writeAndReadModules() {
        writer.writeModule(new ModuleNode("m1", "order", 5, 1, Status.HEALTHY));
        writer.writeModule(new ModuleNode("m2", "inventory", 3, 0, Status.HEALTHY));

        var modules = reader.getModules();
        assertEquals(2, modules.size());
        assertTrue(modules.stream().anyMatch(m -> m.id().equals("m1")));
        assertTrue(modules.stream().anyMatch(m -> m.id().equals("m2")));
    }

    @Test
    void writeAndReadEvents() {
        writer.writeEvent(new EventNode("e1", "OrderPlaced", "m1"));

        var events = reader.getEvents();
        assertEquals(1, events.size());
        assertEquals("OrderPlaced", events.get(0).name());
    }

    @Test
    void writeAndReadEdges() {
        writer.writeEdge(new EventEdge("edge1", "e1", "m1", null, EdgeType.PUBLISHES));
        writer.writeEdge(new EventEdge("edge2", "e1", null, "m2", EdgeType.LISTENS_TO));

        assertEquals(2, reader.getEdges().size());
    }

    @Test
    void getEdgesForEvent_filtersCorrectly() {
        writer.writeEdge(new EventEdge("edge1", "e1", "m1", null, EdgeType.PUBLISHES));
        writer.writeEdge(new EventEdge("edge2", "e2", "m2", null, EdgeType.PUBLISHES));

        var edges = reader.getEdgesForEvent("e1");
        assertEquals(1, edges.size());
        assertEquals("edge1", edges.get(0).id());
    }

    @Test
    void clear_removesAllData() {
        writer.writeModule(new ModuleNode("m1", "order", 1, 0, Status.HEALTHY));
        writer.writeEvent(new EventNode("e1", "OrderPlaced", "m1"));
        writer.writeEdge(new EventEdge("edge1", "e1", "m1", null, EdgeType.PUBLISHES));

        writer.clear();

        assertTrue(reader.getModules().isEmpty());
        assertTrue(reader.getEvents().isEmpty());
        assertTrue(reader.getEdges().isEmpty());
    }

    @Test
    void overwriteModule_updatesEntry() {
        writer.writeModule(new ModuleNode("m1", "order", 5, 1, Status.HEALTHY));
        writer.writeModule(new ModuleNode("m1", "order", 5, 1, Status.WARNING));

        var modules = reader.getModules();
        assertEquals(1, modules.size());
        assertEquals(Status.WARNING, modules.get(0).status());
    }
}
