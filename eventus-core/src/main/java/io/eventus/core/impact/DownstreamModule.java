package io.eventus.core.impact;

public record DownstreamModule(
        String moduleId,
        String moduleName,
        String relationshipType
) {}
