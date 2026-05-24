package io.eventus.core.drift;

public record Drift(
        String id,
        DriftType type,
        DriftSeverity severity,
        String title,
        String description,
        String affectedItemId,
        String affectedItemName,
        long detectedAt
) {}
