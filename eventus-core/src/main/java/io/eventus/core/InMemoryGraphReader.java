package io.eventus.core;

import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;

import java.util.List;

public class InMemoryGraphReader {

    private final InMemoryGraphWriter store;

    public InMemoryGraphReader(InMemoryGraphWriter store) {
        this.store = store;
    }

    public List<ModuleNode> getModules() {
        return List.copyOf(store.modules.values());
    }

    public List<EventNode> getEvents() {
        return List.copyOf(store.events.values());
    }

    public List<EventEdge> getEdges() {
        return List.copyOf(store.edges.values());
    }

    public List<EventEdge> getEdgesForEvent(String eventId) {
        return store.edges.values().stream()
                .filter(e -> e.eventId().equals(eventId))
                .toList();
    }
}
