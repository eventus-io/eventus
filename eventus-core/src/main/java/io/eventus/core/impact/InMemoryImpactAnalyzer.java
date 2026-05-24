package io.eventus.core.impact;

import io.eventus.core.GraphReader;
import io.eventus.core.model.EdgeType;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InMemoryImpactAnalyzer implements ImpactAnalyzer {

    private final GraphReader graphReader;

    public InMemoryImpactAnalyzer(GraphReader graphReader) {
        this.graphReader = graphReader;
    }

    @Override
    public EventImpactResponse analyzeEventImpact(String eventId) {
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

        return new EventImpactResponse(
                event.id(),
                event.name(),
                event.publisherModuleId(),
                affected.size(),
                0,
                affected
        );
    }

    @Override
    public ModuleImpactResponse analyzeModuleImpact(String moduleId) {
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

        return new ModuleImpactResponse(
                module.id(),
                module.name(),
                eventInfos,
                downstream,
                downstreamIds.size()
        );
    }

    private String resolveModuleName(String moduleId) {
        return graphReader.getModules().stream()
                .filter(m -> m.id().equals(moduleId))
                .map(ModuleNode::name)
                .findFirst()
                .orElse(moduleId);
    }
}
