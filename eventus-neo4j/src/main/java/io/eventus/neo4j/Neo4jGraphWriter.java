package io.eventus.neo4j;

import io.eventus.core.GraphWriter;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.GraphModel;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.PublicationRecord;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.List;
import java.util.Map;

public class Neo4jGraphWriter implements GraphWriter {

    private final Neo4jClient client;

    public Neo4jGraphWriter(Neo4jClient client) {
        this.client = client;
    }

    @Override
    public void write(GraphModel model) {
        clear();
        bulkMergeModules(model.modules());
        bulkMergeEvents(model.events());
        bulkMergeEdges(model.edges());
        bulkMergePublications(model.publications());
    }

    @Override
    public void clear() {
        client.query("MATCH (n) DETACH DELETE n").run();
    }

    private void bulkMergeModules(List<ModuleNode> modules) {
        if (modules.isEmpty()) return;
        List<Map<String, Object>> rows = modules.stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.id(),
                        "name", m.name(),
                        "beanCount", m.beanCount(),
                        "aggregateCount", m.aggregateCount(),
                        "status", m.status().name()))
                .toList();
        client.query("""
                UNWIND $rows AS row
                MERGE (n:Module {id: row.id})
                SET n.name = row.name, n.beanCount = row.beanCount,
                    n.aggregateCount = row.aggregateCount, n.status = row.status
                """)
                .bind(rows).to("rows")
                .run();
    }

    private void bulkMergeEvents(List<EventNode> events) {
        if (events.isEmpty()) return;
        List<Map<String, Object>> rows = events.stream()
                .map(e -> Map.<String, Object>of(
                        "id", e.id(),
                        "name", e.name(),
                        "publisherModuleId", e.publisherModuleId()))
                .toList();
        client.query("""
                UNWIND $rows AS row
                MERGE (n:DomainEvent {id: row.id})
                SET n.name = row.name, n.publisherModuleId = row.publisherModuleId
                """)
                .bind(rows).to("rows")
                .run();
    }

    private void bulkMergeEdges(List<EventEdge> edges) {
        if (edges.isEmpty()) return;
        List<Map<String, Object>> rows = edges.stream()
                .map(e -> Map.<String, Object>of(
                        "id", e.id(),
                        "eventId", e.eventId(),
                        "fromModuleId", e.fromModuleId() != null ? e.fromModuleId() : "",
                        "toModuleId", e.toModuleId() != null ? e.toModuleId() : "",
                        "edgeType", e.edgeType().name()))
                .toList();
        client.query("""
                UNWIND $rows AS row
                MERGE (r:EventEdge {id: row.id})
                SET r.eventId = row.eventId, r.fromModuleId = row.fromModuleId,
                    r.toModuleId = row.toModuleId, r.edgeType = row.edgeType
                """)
                .bind(rows).to("rows")
                .run();
    }

    private void bulkMergePublications(List<PublicationRecord> publications) {
        if (publications.isEmpty()) return;
        List<Map<String, Object>> rows = publications.stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.id(),
                        "eventType", p.eventType(),
                        "listenerName", p.listenerName(),
                        "moduleId", p.moduleId(),
                        "status", p.status().name(),
                        "publishedAt", p.publishedAt().toEpochMilli()))
                .toList();
        client.query("""
                UNWIND $rows AS row
                MERGE (n:Publication {id: row.id})
                SET n.eventType = row.eventType, n.listenerName = row.listenerName,
                    n.moduleId = row.moduleId, n.status = row.status,
                    n.publishedAt = row.publishedAt
                """)
                .bind(rows).to("rows")
                .run();
    }
}
