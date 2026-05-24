package io.eventus.core.model;

import java.time.Instant;

public record PublicationRecord(
        String id,
        String eventType,
        String listenerName,
        String moduleId,
        PublicationStatus status,
        Instant publishedAt
) {}
