package io.eventus.spring.actuator;

import io.eventus.core.GraphReader;
import io.eventus.core.model.PublicationRecord;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.List;

@Endpoint(id = "eventus-publications")
public class EventusPublicationsEndpoint {

    private final GraphReader graphReader;

    public EventusPublicationsEndpoint(GraphReader graphReader) {
        this.graphReader = graphReader;
    }

    @ReadOperation
    public List<PublicationRecord> publications() {
        return graphReader.getIncompletePublications();
    }
}
