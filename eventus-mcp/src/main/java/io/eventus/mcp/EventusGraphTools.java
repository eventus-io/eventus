package io.eventus.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.eventus.core.GraphReader;
import io.eventus.core.model.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;

public class EventusGraphTools {

    private final GraphReader graphReader;
    private final ObjectMapper objectMapper;

    public EventusGraphTools(GraphReader graphReader, ObjectMapper objectMapper) {
        this.graphReader = graphReader;
        this.objectMapper = objectMapper;
    }

    @Tool(description = """
            Returns all modules in the application with their status, bean count,
            and aggregate count. Use this to understand the overall module structure
            and identify modules in WARNING or ERROR state.
            """)
    public String getModules() {
        return toJson(graphReader.getModules().stream().map(this::moduleView).toList());
    }

    @Tool(description = """
            Returns all domain events in the application, including the module that
            publishes each event. Use this to understand what events exist and
            which modules produce them.
            """)
    public String getEvents() {
        return toJson(graphReader.getEvents().stream().map(this::eventView).toList());
    }

    @Tool(description = """
            Returns all modules that listen to a specific domain event, given the
            event name (e.g. 'OrderPlaced'). Use this to understand the impact
            of changing or removing an event.
            """)
    public String getEventConsumers(
            @ToolParam(description = "The simple name of the event class, e.g. 'OrderPlaced'")
            String eventName) {
        List<EventNode> matched = graphReader.getEvents().stream()
                .filter(e -> e.name().equals(eventName) || e.id().endsWith("." + eventName))
                .toList();
        if (matched.isEmpty()) {
            return "[]";
        }
        String eventId = matched.getFirst().id();
        List<String> consumers = graphReader.getEdgesForEvent(eventId).stream()
                .filter(edge -> edge.edgeType() == EdgeType.LISTENS_TO)
                .map(EventEdge::toModuleId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (consumers.isEmpty()) {
            return "[]";
        }
        List<ModuleNode> consumerModules = graphReader.getModules().stream()
                .filter(m -> consumers.contains(m.id()))
                .toList();
        return toJson(consumerModules.stream().map(this::moduleView).toList());
    }

    @Tool(description = """
            Returns all event publications that are incomplete or stale.
            Incomplete means the listener has not yet acknowledged the event.
            Stale means it has been incomplete for longer than the configured threshold.
            Use this to diagnose delivery failures and identify which modules are affected.
            """)
    public String getIncompletePublications() {
        return toJson(graphReader.getIncompletePublications().stream().map(this::publicationView).toList());
    }

    @Tool(description = """
            Returns the full dependency profile of a specific module: which events
            it publishes, which events it listens to, and its current health status.
            Use this to understand a module's role in the system before making changes.
            """)
    public String getModuleDependencies(
            @ToolParam(description = "The module name, e.g. 'order' or 'inventory'")
            String moduleName) {
        List<ModuleNode> matched = graphReader.getModules().stream()
                .filter(m -> m.name().equalsIgnoreCase(moduleName) || m.id().equalsIgnoreCase(moduleName))
                .toList();
        if (matched.isEmpty()) {
            return toJson(Map.of("error", "Module not found: " + moduleName));
        }
        ModuleNode module = matched.getFirst();
        List<EventEdge> allEdges = graphReader.getEdges().stream()
                .filter(e -> module.id().equals(e.fromModuleId()) || module.id().equals(e.toModuleId()))
                .toList();

        List<String> publishes = allEdges.stream()
                .filter(e -> e.edgeType() == EdgeType.PUBLISHES)
                .map(e -> eventNameForId(e.eventId()))
                .distinct()
                .toList();
        List<String> listensTo = allEdges.stream()
                .filter(e -> e.edgeType() == EdgeType.LISTENS_TO)
                .map(e -> eventNameForId(e.eventId()))
                .distinct()
                .toList();

        List<Map<String, Object>> edgeViews = allEdges.stream()
                .map(edge -> {
                    var m = new java.util.LinkedHashMap<String, Object>();
                    m.put("event", eventNameForId(edge.eventId()));
                    m.put("type", edge.edgeType().name());
                    if (edge.toModuleId() != null && !edge.toModuleId().isBlank()) {
                        m.put("to", edge.toModuleId());
                    }
                    if (edge.fromModuleId() != null && !edge.fromModuleId().isBlank()
                            && !edge.fromModuleId().equals(module.id())) {
                        m.put("from", edge.fromModuleId());
                    }
                    return (Map<String, Object>) m;
                })
                .toList();

        return toJson(Map.of(
                "module", moduleView(module),
                "publishes", publishes,
                "listensto", listensTo,
                "edges", edgeViews
        ));
    }

    // --- helpers ---

    private String eventNameForId(String eventId) {
        if (eventId == null) return "";
        return graphReader.getEvents().stream()
                .filter(e -> e.id().equals(eventId))
                .map(EventNode::name)
                .findFirst()
                .orElseGet(() -> eventId.contains(".") ? eventId.substring(eventId.lastIndexOf('.') + 1) : eventId);
    }

    private Map<String, Object> moduleView(ModuleNode m) {
        return Map.of(
                "id", m.id(),
                "name", m.name(),
                "status", m.status().name(),
                "beanCount", m.beanCount(),
                "aggregateCount", m.aggregateCount()
        );
    }

    private Map<String, Object> eventView(EventNode e) {
        return Map.of(
                "id", e.id(),
                "name", e.name(),
                "publisherModuleId", e.publisherModuleId()
        );
    }

    private Map<String, Object> publicationView(PublicationRecord p) {
        return Map.of(
                "id", p.id(),
                "eventType", p.eventType(),
                "listenerName", p.listenerName(),
                "moduleId", p.moduleId(),
                "status", p.status().name(),
                "publishedAt", p.publishedAt().toString()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialization failed\"}";
        }
    }
}
