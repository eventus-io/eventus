package io.eventus.neo4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.neo4j.core.Neo4jClient;

public class EventusNeo4jSchemaInitialiser implements ApplicationRunner {

    private final Neo4jClient client;

    public EventusNeo4jSchemaInitialiser(Neo4jClient client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) {
        client.query("CREATE CONSTRAINT module_id IF NOT EXISTS FOR (n:Module) REQUIRE n.id IS UNIQUE").run();
        client.query("CREATE CONSTRAINT event_id IF NOT EXISTS FOR (n:DomainEvent) REQUIRE n.id IS UNIQUE").run();
        client.query("CREATE CONSTRAINT edge_id IF NOT EXISTS FOR (n:EventEdge) REQUIRE n.id IS UNIQUE").run();
        client.query("CREATE CONSTRAINT publication_id IF NOT EXISTS FOR (n:Publication) REQUIRE n.id IS UNIQUE").run();
        client.query("CREATE INDEX module_status IF NOT EXISTS FOR (n:Module) ON (n.status)").run();
        client.query("CREATE INDEX publication_status IF NOT EXISTS FOR (n:Publication) ON (n.status)").run();
    }
}
