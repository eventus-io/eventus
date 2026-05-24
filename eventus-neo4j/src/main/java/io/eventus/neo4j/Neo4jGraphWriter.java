package io.eventus.neo4j;

import io.eventus.core.GraphWriter;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.GraphModel;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.PublicationRecord;
import org.springframework.data.neo4j.core.Neo4jClient;

public class Neo4jGraphWriter implements GraphWriter {

    private final Neo4jClient client;

    public Neo4jGraphWriter(Neo4jClient client) {
        this.client = client;
    }

    @Override
    public void write(GraphModel model) {
        clear();
        model.modules().forEach(this::mergeModule);
        model.events().forEach(this::mergeEvent);
        model.edges().forEach(this::mergeEdge);
        model.publications().forEach(this::mergePublication);
    }

    @Override
    public void clear() {
        client.query("MATCH (n) DETACH DELETE n").run();
    }

    private void mergeModule(ModuleNode m) {
        client.query("""
                MERGE (n:Module {id: $id})
                SET n.name = $name, n.beanCount = $beanCount,
                    n.aggregateCount = $aggregateCount, n.status = $status
                """)
                .bind(m.id()).to("id")
                .bind(m.name()).to("name")
                .bind(m.beanCount()).to("beanCount")
                .bind(m.aggregateCount()).to("aggregateCount")
                .bind(m.status().name()).to("status")
                .run();
    }

    private void mergeEvent(EventNode e) {
        client.query("""
                MERGE (n:DomainEvent {id: $id})
                SET n.name = $name, n.publisherModuleId = $publisherModuleId
                """)
                .bind(e.id()).to("id")
                .bind(e.name()).to("name")
                .bind(e.publisherModuleId()).to("publisherModuleId")
                .run();
    }

    private void mergeEdge(EventEdge e) {
        client.query("""
                MERGE (r:EventEdge {id: $id})
                SET r.eventId = $eventId, r.fromModuleId = $fromModuleId,
                    r.toModuleId = $toModuleId, r.edgeType = $edgeType
                """)
                .bind(e.id()).to("id")
                .bind(e.eventId()).to("eventId")
                .bind(e.fromModuleId()).to("fromModuleId")
                .bind(e.toModuleId()).to("toModuleId")
                .bind(e.edgeType().name()).to("edgeType")
                .run();
    }

    private void mergePublication(PublicationRecord p) {
        client.query("""
                MERGE (n:Publication {id: $id})
                SET n.eventType = $eventType, n.listenerName = $listenerName,
                    n.moduleId = $moduleId, n.status = $status,
                    n.publishedAt = $publishedAt
                """)
                .bind(p.id()).to("id")
                .bind(p.eventType()).to("eventType")
                .bind(p.listenerName()).to("listenerName")
                .bind(p.moduleId()).to("moduleId")
                .bind(p.status().name()).to("status")
                .bind(p.publishedAt().toEpochMilli()).to("publishedAt")
                .run();
    }
}
