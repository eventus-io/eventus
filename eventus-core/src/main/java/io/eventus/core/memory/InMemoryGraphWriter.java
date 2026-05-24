package io.eventus.core.memory;

import io.eventus.core.GraphWriter;
import io.eventus.core.model.GraphModel;

public class InMemoryGraphWriter implements GraphWriter {

    final InMemoryStore store;

    public InMemoryGraphWriter(InMemoryStore store) {
        this.store = store;
    }

    @Override
    public void write(GraphModel model) {
        store.clear();
        model.modules().forEach(m -> store.modules.put(m.id(), m));
        model.events().forEach(e -> store.events.put(e.id(), e));
        store.edges.addAll(model.edges());
        store.publications.addAll(model.publications());
    }

    @Override
    public void clear() {
        store.clear();
    }
}
