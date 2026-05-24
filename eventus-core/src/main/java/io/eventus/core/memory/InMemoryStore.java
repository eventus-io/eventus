package io.eventus.core.memory;

import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.PublicationRecord;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class InMemoryStore {

    final ConcurrentHashMap<String, ModuleNode> modules = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, EventNode> events = new ConcurrentHashMap<>();
    final CopyOnWriteArrayList<EventEdge> edges = new CopyOnWriteArrayList<>();
    final CopyOnWriteArrayList<PublicationRecord> publications = new CopyOnWriteArrayList<>();

    void clear() {
        modules.clear();
        events.clear();
        edges.clear();
        publications.clear();
    }
}
