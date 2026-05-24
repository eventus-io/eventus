package io.eventus.core.impact;

import java.util.List;

public record ModuleImpactResponse(
        String moduleId,
        String moduleName,
        List<EventInfo> publishedEvents,
        List<DownstreamModule> downstreamModules,
        int totalAffectedModules
) {}
