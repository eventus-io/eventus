package io.eventus.core.violations;

import io.eventus.core.GraphCacheAware;
import io.eventus.core.GraphReader;
import io.eventus.core.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class InMemoryViolationAnalyzer implements ViolationAnalyzer, GraphCacheAware {

    private static final Duration CACHE_DURATION = Duration.ofMinutes(5);

    private final GraphReader graphReader;
    private volatile List<Violation> cachedViolations;
    private volatile long lastAnalyzedAt = 0;

    public InMemoryViolationAnalyzer(GraphReader graphReader) {
        this.graphReader = graphReader;
    }

    @Override
    public List<Violation> analyze() {
        if (shouldUseCache()) {
            return cachedViolations;
        }

        List<Violation> violations = new ArrayList<>();
        violations.addAll(detectCircularDependencies());
        violations.addAll(detectHiddenCoupling());
        violations.addAll(detectUnusedEvents());
        violations.addAll(detectFailingListeners());
        violations.addAll(detectStalePublications());

        cachedViolations = Collections.unmodifiableList(violations);
        lastAnalyzedAt = System.currentTimeMillis();
        return cachedViolations;
    }

    private List<Violation> detectCircularDependencies() {
        List<Violation> violations = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (EventNode event : graphReader.getEvents()) {
            Set<String> path = new LinkedHashSet<>();
            if (hasCircularPath(event.id(), visited, path)) {
                violations.add(new Violation(
                        UUID.randomUUID().toString(),
                        ViolationType.CIRCULAR_EVENT_DEPENDENCY,
                        ViolationSeverity.ERROR,
                        "Circular event chain: " + event.name(),
                        "This event and its listeners form a circular dependency",
                        List.of(event.publisherModuleId()),
                        List.of(event.id()),
                        System.currentTimeMillis()
                ));
            }
        }
        return violations;
    }

    private boolean hasCircularPath(String eventId, Set<String> visited, Set<String> currentPath) {
        if (currentPath.contains(eventId)) {
            return true;
        }
        if (visited.contains(eventId)) {
            return false;
        }

        visited.add(eventId);
        currentPath.add(eventId);

        for (EventEdge edge : graphReader.getEdgesForEvent(eventId)) {
            if (edge.edgeType() == EdgeType.LISTENS_TO && edge.toModuleId() != null) {
                for (EventNode published : graphReader.getEvents()) {
                    if (edge.toModuleId().equals(published.publisherModuleId())) {
                        if (hasCircularPath(published.id(), visited, currentPath)) {
                            return true;
                        }
                    }
                }
            }
        }

        currentPath.remove(eventId);
        return false;
    }

    private List<Violation> detectHiddenCoupling() {
        List<Violation> violations = new ArrayList<>();

        for (ModuleNode module : graphReader.getModules()) {
            List<EventEdge> listenerEdges = graphReader.getEdges().stream()
                    .filter(e -> module.id().equals(e.toModuleId()) && e.edgeType() == EdgeType.LISTENS_TO)
                    .toList();

            for (EventEdge edge : listenerEdges) {
                String publisherModuleId = resolvePublisherModuleId(edge.eventId());
                if (publisherModuleId == null || publisherModuleId.equals(module.id())) continue;

                boolean hasDeclaredDependency = graphReader.getEdges().stream()
                        .anyMatch(e -> module.id().equals(e.fromModuleId())
                                && publisherModuleId.equals(e.toModuleId())
                                && e.edgeType() == EdgeType.DEPENDS_ON);

                if (!hasDeclaredDependency) {
                    violations.add(new Violation(
                            UUID.randomUUID().toString(),
                            ViolationType.HIDDEN_COUPLING,
                            ViolationSeverity.WARNING,
                            "Hidden coupling: " + module.name() + " → " + publisherModuleId,
                            module.name() + " listens to events from " + publisherModuleId
                                    + " but does not declare a dependency",
                            List.of(module.id(), publisherModuleId),
                            List.of(edge.eventId()),
                            System.currentTimeMillis()
                    ));
                }
            }
        }
        return violations;
    }

    private String resolvePublisherModuleId(String eventId) {
        return graphReader.getEvents().stream()
                .filter(e -> e.id().equals(eventId))
                .map(EventNode::publisherModuleId)
                .findFirst()
                .orElse(null);
    }

    private List<Violation> detectUnusedEvents() {
        List<Violation> violations = new ArrayList<>();

        for (EventNode event : graphReader.getEvents()) {
            boolean hasListeners = graphReader.getEdgesForEvent(event.id()).stream()
                    .anyMatch(e -> e.edgeType() == EdgeType.LISTENS_TO);

            if (!hasListeners) {
                violations.add(new Violation(
                        UUID.randomUUID().toString(),
                        ViolationType.UNUSED_EVENT,
                        ViolationSeverity.INFO,
                        "Unused event: " + event.name(),
                        event.name() + " is published but has no listeners",
                        List.of(event.publisherModuleId()),
                        List.of(event.id()),
                        System.currentTimeMillis()
                ));
            }
        }
        return violations;
    }

    private List<Violation> detectFailingListeners() {
        List<Violation> violations = new ArrayList<>();
        Set<String> reported = new HashSet<>();

        for (PublicationRecord publication : graphReader.getPublications()) {
            String listener = publication.listenerName();
            if (listener == null || reported.contains(listener)) continue;

            long total = graphReader.getPublications().stream()
                    .filter(p -> listener.equals(p.listenerName()))
                    .count();
            long incomplete = graphReader.getPublications().stream()
                    .filter(p -> listener.equals(p.listenerName()) && p.status() == PublicationStatus.INCOMPLETE)
                    .count();

            if (total > 0 && (incomplete * 100.0 / total) > 80) {
                reported.add(listener);
                violations.add(new Violation(
                        UUID.randomUUID().toString(),
                        ViolationType.FAILING_LISTENER,
                        ViolationSeverity.ERROR,
                        "Failing listener: " + listener,
                        listener + " has >80% failure rate",
                        List.of(publication.moduleId()),
                        List.of(publication.eventType()),
                        System.currentTimeMillis()
                ));
            }
        }
        return violations;
    }

    private List<Violation> detectStalePublications() {
        List<Violation> violations = new ArrayList<>();
        Instant staleThreshold = Instant.now().minus(Duration.ofHours(2));

        for (PublicationRecord publication : graphReader.getPublications()) {
            if (publication.status() == PublicationStatus.STALE
                    && publication.publishedAt().isBefore(staleThreshold)) {
                violations.add(new Violation(
                        UUID.randomUUID().toString(),
                        ViolationType.STALE_PUBLICATION,
                        ViolationSeverity.ERROR,
                        "Stale publication: " + publication.eventType(),
                        "Event publication for " + publication.listenerName()
                                + " is stale (pending since " + publication.publishedAt() + ")",
                        List.of(publication.moduleId()),
                        List.of(publication.eventType()),
                        System.currentTimeMillis()
                ));
            }
        }
        return violations;
    }

    @Override
    public void invalidateCache() {
        cachedViolations = null;
        lastAnalyzedAt = 0;
    }

    private boolean shouldUseCache() {
        return cachedViolations != null
                && (System.currentTimeMillis() - lastAnalyzedAt) < CACHE_DURATION.toMillis();
    }
}
