package io.eventus.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphModel {

    private final List<ModuleNode> modules = new ArrayList<>();
    private final List<EventNode> events = new ArrayList<>();
    private final List<EventEdge> edges = new ArrayList<>();
    private final List<PublicationRecord> publications = new ArrayList<>();

    public void addModule(ModuleNode node) { modules.add(node); }
    public void addEvent(EventNode node) { events.add(node); }
    public void addEdge(EventEdge edge) { edges.add(edge); }
    public void addPublication(PublicationRecord record) { publications.add(record); }

    public List<ModuleNode> modules() { return Collections.unmodifiableList(modules); }
    public List<EventNode> events() { return Collections.unmodifiableList(events); }
    public List<EventEdge> edges() { return Collections.unmodifiableList(edges); }
    public List<PublicationRecord> publications() { return Collections.unmodifiableList(publications); }
}
