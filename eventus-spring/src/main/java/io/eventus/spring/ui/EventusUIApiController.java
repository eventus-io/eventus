package io.eventus.spring.ui;

import io.eventus.core.GraphReader;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.PublicationRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/eventus/api")
public class EventusUIApiController {

    private final GraphReader graphReader;

    public EventusUIApiController(GraphReader graphReader) {
        this.graphReader = graphReader;
    }

    @GetMapping("/graph")
    public GraphResponse getGraph() {
        return new GraphResponse(
                graphReader.getModules(),
                graphReader.getEvents(),
                graphReader.getEdges(),
                graphReader.getPublications()
        );
    }

    public record GraphResponse(
            List<ModuleNode> modules,
            List<EventNode> events,
            List<EventEdge> edges,
            List<PublicationRecord> publications
    ) {}
}
