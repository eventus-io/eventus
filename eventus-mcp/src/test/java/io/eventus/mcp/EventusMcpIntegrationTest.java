package io.eventus.mcp;

import io.eventus.core.GraphReader;
import io.eventus.core.GraphWriter;
import io.eventus.core.memory.InMemoryGraph;
import io.eventus.core.memory.InMemoryGraphReader;
import io.eventus.core.memory.InMemoryGraphWriter;
import io.eventus.core.model.*;
import io.eventus.mcp.autoconfigure.EventusMcpAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that EventusGraphTools beans are wired correctly in a full Spring Boot context
 * and that tools return accurate data from the live graph.
 *
 * Note: MCP HTTP endpoint testing requires an SSE session setup (Spring AI 1.0 transport);
 * that path is covered by manual integration testing with Claude Desktop.
 */
@SpringBootTest(
        classes = EventusMcpIntegrationTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.ai.mcp.server.stdio=true"
)
class EventusMcpIntegrationTest {

    @SpringBootApplication
    @ImportAutoConfiguration(EventusMcpAutoConfiguration.class)
    static class TestApp {

        @Bean
        InMemoryGraphWriter inMemoryGraphWriter() {
            return InMemoryGraph.writer();
        }

        @Bean
        InMemoryGraphReader inMemoryGraphReader(InMemoryGraphWriter writer) {
            return InMemoryGraph.reader(writer);
        }

        @Bean
        GraphWriter graphWriter(InMemoryGraphWriter writer) {
            return writer;
        }

        @Bean
        GraphReader graphReader(InMemoryGraphReader reader) {
            return reader;
        }
    }

    @Autowired
    GraphWriter graphWriter;

    @Autowired
    EventusGraphTools tools;

    @Autowired
    ToolCallbackProvider eventusToolCallbackProvider;

    @BeforeEach
    void populateGraph() {
        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "order", 3, 1, ModuleStatus.HEALTHY));
        model.addModule(new ModuleNode("inventory", "inventory", 2, 0, ModuleStatus.WARNING));
        model.addEvent(new EventNode("io.example.OrderPlaced", "OrderPlaced", "order"));
        model.addEdge(new EventEdge("e1", "io.example.OrderPlaced", "order", null, EdgeType.PUBLISHES));
        model.addEdge(new EventEdge("e2", "io.example.OrderPlaced", null, "inventory", EdgeType.LISTENS_TO));
        graphWriter.write(model);
    }

    @Test
    void toolsBeanIsCreated() {
        assertThat(tools).isNotNull();
        assertThat(eventusToolCallbackProvider).isNotNull();
    }

    @Test
    void toolCallbackProviderExposesAllFiveTools() {
        var callbacks = eventusToolCallbackProvider.getToolCallbacks();
        var names = java.util.Arrays.stream(callbacks)
                .map(c -> c.getToolDefinition().name())
                .toList();
        assertThat(names).containsExactlyInAnyOrder(
                "getModules", "getEvents", "getEventConsumers",
                "getIncompletePublications", "getModuleDependencies");
    }

    @Test
    void getModulesReturnsLiveData() {
        assertThat(tools.getModules()).contains("order").contains("inventory");
    }

    @Test
    void getEventConsumersReturnsInventoryForOrderPlaced() {
        assertThat(tools.getEventConsumers("OrderPlaced")).contains("inventory");
    }
}
