package io.eventus.core.impact;

import io.eventus.core.GraphCacheAware;
import io.eventus.core.GraphReader;
import io.eventus.core.model.EdgeType;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryImpactAnalyzer implements ImpactAnalyzer, GraphCacheAware {

    private static final Duration CACHE_DURATION = Duration.ofMinutes(5);

    private final GraphReader graphReader;
    private final Map<String, EventImpactResponse> eventCache = new ConcurrentHashMap<>();
    private final Map<String, ModuleImpactResponse> moduleCache = new ConcurrentHashMap<>();
    private volatile long lastInvalidatedAt = 0;

    public InMemoryImpactAnalyzer(GraphReader graphReader) {
        this.graphReader = graphReader;
    }

    @Override
    public void invalidateCache() {
        eventCache.clear();
        moduleCache.clear();
        lastInvalidatedAt = System.currentTimeMillis();
    }

    private boolean cacheIsStale() {
        return (System.currentTimeMillis() - lastInvalidatedAt) > CACHE_DURATION.toMillis();
    }

    @Override
    public EventImpactResponse analyzeEventImpact(String eventId) {
        if (!cacheIsStale()) {
            EventImpactResponse cached = eventCache.get(eventId);
            if (cached != null) return cached;
        } else {
            eventCache.clear();
        }

        EventNode event = graphReader.getEvents().stream()
                .filter(e -> e.id().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new EventNotFoundException(eventId));

        List<AffectedModule> affected = graphReader.getEdgesForEvent(eventId).stream()
                .filter(e -> e.edgeType() == EdgeType.LISTENS_TO && e.toModuleId() != null)
                .map(e -> new AffectedModule(
                        e.toModuleId(),
                        resolveModuleName(e.toModuleId()),
                        e.edgeType().name(),
                        true
                ))
                .distinct()
                .toList();

        EventImpactResponse result = new EventImpactResponse(
                event.id(),
                event.name(),
                event.publisherModuleId(),
                affected.size(),
                0,
                affected
        );
        eventCache.put(eventId, result);
        return result;
    }

    @Override
    public ModuleImpactResponse analyzeModuleImpact(String moduleId) {
        if (!cacheIsStale()) {
            ModuleImpactResponse cached = moduleCache.get(moduleId);
            if (cached != null) return cached;
        } else {
            moduleCache.clear();
        }

        ModuleNode module = graphReader.getModules().stream()
                .filter(m -> m.id().equals(moduleId))
                .findFirst()
                .orElseThrow(() -> new ModuleNotFoundException(moduleId));

        List<EventInfo> eventInfos = graphReader.getEvents().stream()
                .filter(e -> moduleId.equals(e.publisherModuleId()))
                .map(event -> {
                    List<String> listeners = graphReader.getEdgesForEvent(event.id()).stream()
                            .filter(e -> e.edgeType() == EdgeType.LISTENS_TO && e.toModuleId() != null)
                            .map(EventEdge::toModuleId)
                            .distinct()
                            .toList();
                    return new EventInfo(event.id(), event.name(), listeners.size(), listeners);
                })
                .toList();

        Set<String> downstreamIds = new HashSet<>();
        eventInfos.forEach(ei -> downstreamIds.addAll(ei.listenerModuleIds()));

        List<DownstreamModule> downstream = downstreamIds.stream()
                .map(id -> new DownstreamModule(id, resolveModuleName(id), "EVENT_LISTENER"))
                .toList();

        ModuleImpactResponse result = new ModuleImpactResponse(
                module.id(),
                module.name(),
                eventInfos,
                downstream,
                downstreamIds.size()
        );
        moduleCache.put(moduleId, result);
        return result;
    }

    private String resolveModuleName(String moduleId) {
        return graphReader.getModules().stream()
                .filter(m -> m.id().equals(moduleId))
                .map(ModuleNode::name)
                .findFirst()
                .orElse(moduleId);
    }
}
