package io.eventus.core.impact;

public record AffectedModule(
        String moduleId,
        String moduleName,
        String relationshipType,
        boolean isDirectListener
) {}
