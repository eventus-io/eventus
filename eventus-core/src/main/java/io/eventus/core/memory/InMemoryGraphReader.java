package io.eventus.core.memory;

import io.eventus.core.GraphReader;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.PublicationRecord;
import io.eventus.core.model.PublicationStatus;

import java.util.List;

public class InMemoryGraphReader implements GraphReader {

    private final InMemoryStore store;

    public InMemoryGraphReader(InMemoryStore store) {
        this.store = store;
    }

    @Override
    public List<ModuleNode> getModules() {
        return List.copyOf(store.modules.values());
    }

    @Override
    public List<EventNode> getEvents() {
        return List.copyOf(store.events.values());
    }

    @Override
    public List<EventEdge> getEdges() {
        return List.copyOf(store.edges);
    }

    @Override
    public List<EventEdge> getEdgesForEvent(String eventId) {
        return store.edges.stream()
                .filter(e -> e.eventId().equals(eventId))
                .toList();
    }

    @Override
    public List<PublicationRecord> getPublications() {
        return List.copyOf(store.publications);
    }

    @Override
    public List<PublicationRecord> getIncompletePublications() {
        return store.publications.stream()
                .filter(p -> p.status() != PublicationStatus.COMPLETED)
                .toList();
    }
}
