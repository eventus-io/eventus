package io.eventus.spring.actuator;

import io.eventus.core.GraphReader;
import io.eventus.core.model.EventNode;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.List;

@Endpoint(id = "eventus-events")
public class EventusEventsEndpoint {

    private final GraphReader graphReader;

    public EventusEventsEndpoint(GraphReader graphReader) {
        this.graphReader = graphReader;
    }

    @ReadOperation
    public List<EventNode> events() {
        return graphReader.getEvents();
    }
}
