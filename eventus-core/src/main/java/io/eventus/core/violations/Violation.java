package io.eventus.core.violations;

import java.util.List;

public record Violation(
        String id,
        ViolationType type,
        ViolationSeverity severity,
        String title,
        String description,
        List<String> affectedModuleIds,
        List<String> affectedEventIds,
        long detectedAt
) {}
