package io.eventus.spring.actuator;

import io.eventus.core.GraphReader;
import io.eventus.core.model.ModuleNode;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.List;

@Endpoint(id = "eventus-modules")
public class EventusModulesEndpoint {

    private final GraphReader graphReader;

    public EventusModulesEndpoint(GraphReader graphReader) {
        this.graphReader = graphReader;
    }

    @ReadOperation
    public List<ModuleNode> modules() {
        return graphReader.getModules();
    }
}
