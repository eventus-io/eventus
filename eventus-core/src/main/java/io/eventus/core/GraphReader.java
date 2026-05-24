package io.eventus.core;

import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.PublicationRecord;

import java.util.List;

public interface GraphReader {
    List<ModuleNode> getModules();
    List<EventNode> getEvents();
    List<EventEdge> getEdges();
    List<EventEdge> getEdgesForEvent(String eventId);
    List<PublicationRecord> getPublications();
    List<PublicationRecord> getIncompletePublications();
}
