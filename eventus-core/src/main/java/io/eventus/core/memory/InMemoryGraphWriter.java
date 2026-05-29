package io.eventus.core.memory;

import io.eventus.core.GraphCacheAware;
import io.eventus.core.GraphWriter;
import io.eventus.core.model.GraphModel;

import java.util.ArrayList;
import java.util.List;

public class InMemoryGraphWriter implements GraphWriter {

    final InMemoryStore store;
    private final List<GraphCacheAware> cacheListeners = new ArrayList<>();

    public InMemoryGraphWriter(InMemoryStore store) {
        this.store = store;
    }

    public void registerCacheAware(GraphCacheAware listener) {
        cacheListeners.add(listener);
    }

    @Override
    public void write(GraphModel model) {
        store.clear();
        model.modules().forEach(m -> store.modules.put(m.id(), m));
        model.events().forEach(e -> store.events.put(e.id(), e));
        store.edges.addAll(model.edges());
        store.publications.addAll(model.publications());
        cacheListeners.forEach(GraphCacheAware::invalidateCache);
    }

    public void updatePublications(java.util.List<io.eventus.core.model.PublicationRecord> publications) {
        store.publications.clear();
        store.publications.addAll(publications);
        cacheListeners.forEach(GraphCacheAware::invalidateCache);
    }

    @Override
    public void clear() {
        store.clear();
        cacheListeners.forEach(GraphCacheAware::invalidateCache);
    }
}
