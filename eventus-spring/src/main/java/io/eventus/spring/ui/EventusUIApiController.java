package io.eventus.spring.ui;

import io.eventus.core.GraphReader;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.PublicationRecord;
import io.eventus.spring.publications.ModulithPublicationBridge;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/eventus/api")
public class EventusUIApiController {

    private final GraphReader graphReader;
    private final Optional<ModulithPublicationBridge> publicationBridge;

    public EventusUIApiController(GraphReader graphReader, Optional<ModulithPublicationBridge> publicationBridge) {
        this.graphReader = graphReader;
        this.publicationBridge = publicationBridge;
    }

    @GetMapping("/graph")
    public GraphResponse getGraph() {
        publicationBridge.ifPresent(ModulithPublicationBridge::sync);
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
