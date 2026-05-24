package io.eventus.core;

import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;

import java.util.concurrent.ConcurrentHashMap;

public class InMemoryGraphWriter implements GraphWriter {

    final ConcurrentHashMap<String, ModuleNode> modules = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, EventNode> events = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, EventEdge> edges = new ConcurrentHashMap<>();

    @Override
    public void writeModule(ModuleNode node) {
        modules.put(node.id(), node);
    }

    @Override
    public void writeEvent(EventNode node) {
        events.put(node.id(), node);
    }

    @Override
    public void writeEdge(EventEdge edge) {
        edges.put(edge.id(), edge);
    }

    @Override
    public void clear() {
        modules.clear();
        events.clear();
        edges.clear();
    }
}
