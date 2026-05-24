package io.eventus.mcp;

import io.eventus.core.GraphReader;
import io.eventus.core.model.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventusGraphTools {

    private final GraphReader graphReader;

    public EventusGraphTools(GraphReader graphReader) {
        this.graphReader = graphReader;
    }

    @Tool(description = """
            Returns all modules in the application with their status, bean count,
            and aggregate count. Use this to understand the overall module structure
            and identify modules in WARNING or ERROR state.
            """)
    public String getModules() {
        return serializeModules(graphReader.getModules());
    }

    @Tool(description = """
            Returns all domain events in the application, including the module that
            publishes each event. Use this to understand what events exist and
            which modules produce them.
            """)
    public String getEvents() {
        return serializeEvents(graphReader.getEvents());
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
        return serializeModules(consumerModules);
    }

    @Tool(description = """
            Returns all event publications that are incomplete or stale.
            Incomplete means the listener has not yet acknowledged the event.
            Stale means it has been incomplete for longer than the configured threshold.
            Use this to diagnose delivery failures and identify which modules are affected.
            """)
    public String getIncompletePublications() {
        return serializePublications(graphReader.getIncompletePublications());
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
            return "{\"error\":\"Module not found: " + escape(moduleName) + "\"}";
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

        var sb = new StringBuilder();
        sb.append("{");
        sb.append("\"module\":").append(serializeModule(module)).append(",");
        sb.append("\"publishes\":").append(serializeStringList(publishes)).append(",");
        sb.append("\"listensto\":").append(serializeStringList(listensTo)).append(",");
        sb.append("\"edges\":[");
        for (int i = 0; i < allEdges.size(); i++) {
            if (i > 0) sb.append(",");
            EventEdge edge = allEdges.get(i);
            String eventName = eventNameForId(edge.eventId());
            sb.append("{");
            sb.append("\"event\":\"").append(escape(eventName)).append("\",");
            sb.append("\"type\":\"").append(edge.edgeType()).append("\"");
            if (edge.toModuleId() != null && !edge.toModuleId().isBlank()) {
                sb.append(",\"to\":\"").append(escape(edge.toModuleId())).append("\"");
            }
            if (edge.fromModuleId() != null && !edge.fromModuleId().isBlank()
                    && !edge.fromModuleId().equals(module.id())) {
                sb.append(",\"from\":\"").append(escape(edge.fromModuleId())).append("\"");
            }
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    // --- serialization helpers ---

    private String eventNameForId(String eventId) {
        if (eventId == null) return "";
        return graphReader.getEvents().stream()
                .filter(e -> e.id().equals(eventId))
                .map(EventNode::name)
                .findFirst()
                .orElseGet(() -> eventId.contains(".") ? eventId.substring(eventId.lastIndexOf('.') + 1) : eventId);
    }

    private static String serializeModules(List<ModuleNode> modules) {
        if (modules.isEmpty()) return "[]";
        var sb = new StringBuilder("[");
        for (int i = 0; i < modules.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(serializeModule(modules.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String serializeModule(ModuleNode m) {
        return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"status\":\"%s\",\"beanCount\":%d,\"aggregateCount\":%d}",
                escape(m.id()), escape(m.name()), m.status(), m.beanCount(), m.aggregateCount());
    }

    private static String serializeEvents(List<EventNode> events) {
        if (events.isEmpty()) return "[]";
        var sb = new StringBuilder("[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) sb.append(",");
            EventNode e = events.get(i);
            sb.append(String.format(
                    "{\"id\":\"%s\",\"name\":\"%s\",\"publisherModuleId\":\"%s\"}",
                    escape(e.id()), escape(e.name()), escape(e.publisherModuleId())));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String serializePublications(List<PublicationRecord> pubs) {
        if (pubs.isEmpty()) return "[]";
        var sb = new StringBuilder("[");
        for (int i = 0; i < pubs.size(); i++) {
            if (i > 0) sb.append(",");
            PublicationRecord p = pubs.get(i);
            sb.append(String.format(
                    "{\"id\":\"%s\",\"eventType\":\"%s\",\"listenerName\":\"%s\",\"moduleId\":\"%s\",\"status\":\"%s\",\"publishedAt\":\"%s\"}",
                    escape(p.id()), escape(p.eventType()), escape(p.listenerName()),
                    escape(p.moduleId()), p.status(), p.publishedAt()));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String serializeStringList(List<String> items) {
        if (items.isEmpty()) return "[]";
        var sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escape(items.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
