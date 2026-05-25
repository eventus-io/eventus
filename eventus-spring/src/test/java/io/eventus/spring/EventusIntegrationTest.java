package io.eventus.spring;

import io.eventus.core.GraphWriter;
import io.eventus.core.model.*;
import io.eventus.spring.test.TestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class EventusIntegrationTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @Autowired
    GraphWriter graphWriter;

    @BeforeEach
    void populateGraph() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "Order", 3, 1, ModuleStatus.HEALTHY));
        model.addModule(new ModuleNode("inventory", "Inventory", 2, 0, ModuleStatus.HEALTHY));
        model.addEvent(new EventNode(
                "io.eventus.spring.test.order.OrderPlaced", "OrderPlaced", "order"));
        model.addEdge(new EventEdge(
                "e1", "io.eventus.spring.test.order.OrderPlaced",
                "order", null, EdgeType.PUBLISHES));
        model.addEdge(new EventEdge(
                "e2", "io.eventus.spring.test.order.OrderPlaced",
                null, "inventory", EdgeType.LISTENS_TO));
        graphWriter.write(model);
    }

    @Test
    void modulesEndpointReturnsBothModules() throws Exception {
        mockMvc.perform(get("/actuator/eventus-modules"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/vnd.spring-boot.actuator.v3+json"))
                .andExpect(jsonPath("$[*].id", hasItems("order", "inventory")));
    }

    @Test
    void eventsEndpointReturnsOrderPlacedEvent() throws Exception {
        mockMvc.perform(get("/actuator/eventus-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("OrderPlaced")))
                .andExpect(jsonPath("$[?(@.name=='OrderPlaced')].publisherModuleId", hasItem("order")));
    }

    @Test
    void publicationsEndpointReturnsEmptyListWhenNoneIncomplete() throws Exception {
        mockMvc.perform(get("/actuator/eventus-publications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
