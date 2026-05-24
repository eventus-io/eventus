package io.eventus.spring;

import io.eventus.core.InMemoryGraphWriter;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventEdge.EdgeType;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.ModuleNode.Status;
import io.eventus.spring.testapp.TestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "management.endpoints.web.exposure.include=*",
                "management.endpoint.eventus.enabled=true"
        }
)
class EventusSpringIntegrationTest {

    @LocalManagementPort
    int managementPort;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    InMemoryGraphWriter writer;

    @BeforeEach
    void populateGraph() {
        writer.clear();
        writer.writeModule(new ModuleNode("order", "Order", 2, 0, Status.HEALTHY));
        writer.writeModule(new ModuleNode("inventory", "Inventory", 1, 0, Status.HEALTHY));
        writer.writeEvent(new EventNode(
                "io.eventus.spring.testapp.order.OrderPlaced",
                "OrderPlaced",
                "order"));
        writer.writeEdge(new EventEdge(
                "edge-1",
                "io.eventus.spring.testapp.order.OrderPlaced",
                "order",
                null,
                EdgeType.PUBLISHES));
        writer.writeEdge(new EventEdge(
                "edge-2",
                "io.eventus.spring.testapp.order.OrderPlaced",
                null,
                "inventory",
                EdgeType.LISTENS_TO));
    }

    @Test
    void modulesEndpointReturnsBothModules() {
        String url = "http://localhost:" + managementPort + "/actuator/eventus/modules";
        String response = restTemplate.getForObject(url, String.class);

        assertThat(response).isNotNull();
        assertThat(response).contains("order");
        assertThat(response).contains("inventory");
    }

    @Test
    void eventsEndpointReturnsOrderPlacedEvent() {
        String url = "http://localhost:" + managementPort + "/actuator/eventus/events";
        String response = restTemplate.getForObject(url, String.class);

        assertThat(response).isNotNull();
        assertThat(response).contains("OrderPlaced");
    }
}
