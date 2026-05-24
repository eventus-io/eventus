package io.eventus.core;

import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;

public interface GraphWriter {
    void writeModule(ModuleNode node);
    void writeEvent(EventNode node);
    void writeEdge(EventEdge edge);
    void clear();
}
