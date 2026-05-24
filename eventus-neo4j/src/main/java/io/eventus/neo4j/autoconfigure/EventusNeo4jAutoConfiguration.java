package io.eventus.neo4j.autoconfigure;

import io.eventus.core.GraphReader;
import io.eventus.core.GraphWriter;
import io.eventus.neo4j.EventusNeo4jSchemaInitialiser;
import io.eventus.neo4j.Neo4jGraphReader;
import io.eventus.neo4j.Neo4jGraphWriter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.Neo4jClient;

@AutoConfiguration
@ConditionalOnClass(Neo4jClient.class)
@ConditionalOnProperty(prefix = "eventus.neo4j", name = "enabled", matchIfMissing = true)
public class EventusNeo4jAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(GraphWriter.class)
    public GraphWriter neo4jGraphWriter(Neo4jClient client) {
        return new Neo4jGraphWriter(client);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(GraphReader.class)
    public GraphReader neo4jGraphReader(Neo4jClient client) {
        return new Neo4jGraphReader(client);
    }

    @Bean
    public EventusNeo4jSchemaInitialiser neo4jSchemaInitialiser(Neo4jClient client) {
        return new EventusNeo4jSchemaInitialiser(client);
    }
}
