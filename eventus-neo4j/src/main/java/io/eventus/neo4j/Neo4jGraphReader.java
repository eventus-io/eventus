package io.eventus.neo4j;

import io.eventus.core.GraphReader;
import io.eventus.core.model.EdgeType;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.ModuleStatus;
import io.eventus.core.model.PublicationRecord;
import io.eventus.core.model.PublicationStatus;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.time.Instant;
import java.util.List;

public class Neo4jGraphReader implements GraphReader {

    private final Neo4jClient client;

    public Neo4jGraphReader(Neo4jClient client) {
        this.client = client;
    }

    @Override
    public List<ModuleNode> getModules() {
        return client.query("MATCH (n:Module) RETURN n.id, n.name, n.beanCount, n.aggregateCount, n.status")
                .fetchAs(ModuleNode.class)
                .mappedBy((ts, record) -> new ModuleNode(
                        record.get("n.id").asString(),
                        record.get("n.name").asString(),
                        record.get("n.beanCount").asInt(0),
                        record.get("n.aggregateCount").asInt(0),
                        ModuleStatus.valueOf(record.get("n.status").asString())))
                .all().stream().toList();
    }

    @Override
    public List<EventNode> getEvents() {
        return client.query("MATCH (n:DomainEvent) RETURN n.id, n.name, n.publisherModuleId")
                .fetchAs(EventNode.class)
                .mappedBy((ts, record) -> new EventNode(
                        record.get("n.id").asString(),
                        record.get("n.name").asString(),
                        record.get("n.publisherModuleId").asString()))
                .all().stream().toList();
    }

    @Override
    public List<EventEdge> getEdges() {
        return client.query("MATCH (n:EventEdge) RETURN n.id, n.eventId, n.fromModuleId, n.toModuleId, n.edgeType")
                .fetchAs(EventEdge.class)
                .mappedBy((ts, record) -> new EventEdge(
                        record.get("n.id").asString(),
                        record.get("n.eventId").asString(),
                        nullableString(record, "n.fromModuleId"),
                        nullableString(record, "n.toModuleId"),
                        EdgeType.valueOf(record.get("n.edgeType").asString())))
                .all().stream().toList();
    }

    @Override
    public List<EventEdge> getEdgesForEvent(String eventId) {
        return client.query("MATCH (n:EventEdge {eventId: $eventId}) RETURN n.id, n.eventId, n.fromModuleId, n.toModuleId, n.edgeType")
                .bind(eventId).to("eventId")
                .fetchAs(EventEdge.class)
                .mappedBy((ts, record) -> new EventEdge(
                        record.get("n.id").asString(),
                        record.get("n.eventId").asString(),
                        nullableString(record, "n.fromModuleId"),
                        nullableString(record, "n.toModuleId"),
                        EdgeType.valueOf(record.get("n.edgeType").asString())))
                .all().stream().toList();
    }

    @Override
    public List<PublicationRecord> getPublications() {
        return client.query("MATCH (n:Publication) RETURN n.id, n.eventType, n.listenerName, n.moduleId, n.status, n.publishedAt")
                .fetchAs(PublicationRecord.class)
                .mappedBy((ts, record) -> new PublicationRecord(
                        record.get("n.id").asString(),
                        record.get("n.eventType").asString(),
                        record.get("n.listenerName").asString(),
                        record.get("n.moduleId").asString(),
                        PublicationStatus.valueOf(record.get("n.status").asString()),
                        Instant.ofEpochMilli(record.get("n.publishedAt").asLong())))
                .all().stream().toList();
    }

    @Override
    public List<PublicationRecord> getIncompletePublications() {
        return client.query("MATCH (n:Publication) WHERE n.status IN ['INCOMPLETE', 'STALE'] RETURN n.id, n.eventType, n.listenerName, n.moduleId, n.status, n.publishedAt")
                .fetchAs(PublicationRecord.class)
                .mappedBy((ts, record) -> new PublicationRecord(
                        record.get("n.id").asString(),
                        record.get("n.eventType").asString(),
                        record.get("n.listenerName").asString(),
                        record.get("n.moduleId").asString(),
                        PublicationStatus.valueOf(record.get("n.status").asString()),
                        Instant.ofEpochMilli(record.get("n.publishedAt").asLong())))
                .all().stream().toList();
    }

    private String nullableString(org.neo4j.driver.Record record, String key) {
        var val = record.get(key);
        return val.isNull() ? null : val.asString();
    }
}
