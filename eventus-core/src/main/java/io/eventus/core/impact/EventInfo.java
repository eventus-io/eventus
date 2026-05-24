package io.eventus.core.impact;

import java.util.List;

public record EventInfo(
        String eventId,
        String eventName,
        int directListeners,
        List<String> listenerModuleIds
) {}
