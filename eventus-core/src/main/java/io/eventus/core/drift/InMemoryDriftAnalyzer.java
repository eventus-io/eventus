package io.eventus.core.drift;

import io.eventus.core.GraphReader;
import io.eventus.core.model.EdgeType;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryDriftAnalyzer implements DriftAnalyzer {

    private final GraphReader currentGraph;
    private final BaselineManager baselineManager;

    public InMemoryDriftAnalyzer(GraphReader currentGraph, BaselineManager baselineManager) {
        this.currentGraph = currentGraph;
        this.baselineManager = baselineManager;
    }

    @Override
    public ArchitecturalDriftReport analyzeDrift() {
        BaselineSnapshot baseline = baselineManager.loadBaseline();
        if (baseline == null) {
            return new ArchitecturalDriftReport(List.of(), 0, 0, System.currentTimeMillis());
        }

        List<Drift> drifts = new ArrayList<>();
        drifts.addAll(detectModuleChanges(baseline));
        drifts.addAll(detectEventChanges(baseline));
        drifts.addAll(detectEdgeChanges(baseline));

        int breachingCount = (int) drifts.stream()
                .filter(d -> d.severity() == DriftSeverity.BREAKING)
                .count();

        return new ArchitecturalDriftReport(drifts, drifts.size(), breachingCount, System.currentTimeMillis());
    }

    private List<Drift> detectModuleChanges(BaselineSnapshot baseline) {
        List<Drift> drifts = new ArrayList<>();

        Set<String> baselineIds = baseline.modules().stream().map(ModuleNode::id).collect(Collectors.toSet());
        Set<String> currentIds = currentGraph.getModules().stream().map(ModuleNode::id).collect(Collectors.toSet());

        baselineIds.stream().filter(id -> !currentIds.contains(id)).forEach(id -> {
            String name = baseline.modules().stream()
                    .filter(m -> m.id().equals(id)).map(ModuleNode::name).findFirst().orElse(id);
            boolean isDepended = baseline.events().stream()
                    .filter(ev -> id.equals(ev.publisherModuleId()))
                    .anyMatch(ev -> baseline.edges().stream()
                            .anyMatch(e -> e.edgeType() == EdgeType.LISTENS_TO
                                    && ev.id().equals(e.eventId())));
            drifts.add(new Drift(UUID.randomUUID().toString(), DriftType.MODULE_REMOVED,
                    isDepended ? DriftSeverity.BREAKING : DriftSeverity.MODERATE,
                    "Module removed: " + name, name + " was in baseline but is no longer present",
                    id, name, System.currentTimeMillis()));
        });

        currentIds.stream().filter(id -> !baselineIds.contains(id)).forEach(id -> {
            String name = currentGraph.getModules().stream()
                    .filter(m -> m.id().equals(id)).map(ModuleNode::name).findFirst().orElse(id);
            drifts.add(new Drift(UUID.randomUUID().toString(), DriftType.MODULE_ADDED,
                    DriftSeverity.MODERATE,
                    "Module added: " + name, name + " is new since baseline",
                    id, name, System.currentTimeMillis()));
        });

        return drifts;
    }

    private List<Drift> detectEventChanges(BaselineSnapshot baseline) {
        List<Drift> drifts = new ArrayList<>();

        Set<String> baselineIds = baseline.events().stream().map(EventNode::id).collect(Collectors.toSet());
        Set<String> currentIds = currentGraph.getEvents().stream().map(EventNode::id).collect(Collectors.toSet());

        baselineIds.stream().filter(id -> !currentIds.contains(id)).forEach(id -> {
            String name = baseline.events().stream()
                    .filter(e -> e.id().equals(id)).map(EventNode::name).findFirst().orElse(id);
            boolean isListened = baseline.edges().stream()
                    .anyMatch(e -> id.equals(e.eventId()) && e.edgeType() == EdgeType.LISTENS_TO);
            drifts.add(new Drift(UUID.randomUUID().toString(), DriftType.EVENT_REMOVED,
                    isListened ? DriftSeverity.BREAKING : DriftSeverity.MODERATE,
                    "Event removed: " + name, name + " was published in baseline but is no longer",
                    id, name, System.currentTimeMillis()));
        });

        currentIds.stream().filter(id -> !baselineIds.contains(id)).forEach(id -> {
            String name = currentGraph.getEvents().stream()
                    .filter(e -> e.id().equals(id)).map(EventNode::name).findFirst().orElse(id);
            drifts.add(new Drift(UUID.randomUUID().toString(), DriftType.EVENT_ADDED,
                    DriftSeverity.MINOR,
                    "Event added: " + name, name + " is new since baseline",
                    id, name, System.currentTimeMillis()));
        });

        return drifts;
    }

    private List<Drift> detectEdgeChanges(BaselineSnapshot baseline) {
        List<Drift> drifts = new ArrayList<>();

        Set<String> baselineIds = baseline.edges().stream().map(EventEdge::id).collect(Collectors.toSet());
        Set<String> currentIds = currentGraph.getEdges().stream().map(EventEdge::id).collect(Collectors.toSet());

        baseline.edges().stream().filter(e -> !currentIds.contains(e.id())).forEach(edge -> {
            String label = edge.toModuleId() != null ? edge.toModuleId() : edge.fromModuleId();
            drifts.add(new Drift(UUID.randomUUID().toString(), DriftType.LISTENER_REMOVED,
                    DriftSeverity.MODERATE,
                    "Edge removed: " + label, label + " no longer has this edge",
                    edge.eventId(), edge.eventId(), System.currentTimeMillis()));
        });

        currentGraph.getEdges().stream().filter(e -> !baselineIds.contains(e.id())).forEach(edge -> {
            String label = edge.toModuleId() != null ? edge.toModuleId() : edge.fromModuleId();
            drifts.add(new Drift(UUID.randomUUID().toString(), DriftType.LISTENER_ADDED,
                    DriftSeverity.MINOR,
                    "Edge added: " + label, label + " now has this new edge",
                    edge.eventId(), edge.eventId(), System.currentTimeMillis()));
        });

        return drifts;
    }
}
