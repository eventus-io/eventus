package io.eventus.core.model;

public record EventEdge(
        String id,
        String eventId,
        String fromModuleId,
        String toModuleId,
        EdgeType edgeType
) {}
