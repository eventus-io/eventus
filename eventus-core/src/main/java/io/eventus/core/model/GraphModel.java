package io.eventus.core.model;

import java.util.List;

public record GraphModel(
        List<ModuleNode> modules,
        List<EventNode> events,
        List<EventEdge> edges
) {
    public static GraphModel empty() {
        return new GraphModel(List.of(), List.of(), List.of());
    }
}
