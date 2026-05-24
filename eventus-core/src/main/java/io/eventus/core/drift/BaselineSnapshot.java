package io.eventus.core.drift;

import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;

import java.util.List;

public record BaselineSnapshot(
        List<ModuleNode> modules,
        List<EventNode> events,
        List<EventEdge> edges,
        long capturedAt
) {}
