package io.eventus.core.impact;

import java.util.List;

public record EventImpactResponse(
        String eventId,
        String eventName,
        String publisherModuleId,
        int directListeners,
        int indirectConsumers,
        List<AffectedModule> affectedModules
) {}
