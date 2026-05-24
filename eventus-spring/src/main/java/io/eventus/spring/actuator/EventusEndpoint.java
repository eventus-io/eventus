package io.eventus.spring.actuator;

import io.eventus.core.InMemoryGraphReader;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.List;
import java.util.Map;

@Endpoint(id = "eventus")
public class EventusEndpoint {

    private final InMemoryGraphReader reader;

    public EventusEndpoint(InMemoryGraphReader reader) {
        this.reader = reader;
    }

    @ReadOperation
    public Object invoke(@Selector String section) {
        return switch (section) {
            case "modules" -> modules();
            case "events" -> events();
            case "edges" -> edges();
            case "publications" -> Map.of("note", "EventPublicationRepository integration coming in v0.2");
            default -> null;
        };
    }

    private List<ModuleNode> modules() {
        return reader.getModules();
    }

    private List<EventNode> events() {
        return reader.getEvents();
    }

    private List<EventEdge> edges() {
        return reader.getEdges();
    }
}
