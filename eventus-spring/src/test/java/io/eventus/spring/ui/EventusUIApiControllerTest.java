package io.eventus.spring.ui;

import io.eventus.core.GraphWriter;
import io.eventus.core.model.*;
import io.eventus.spring.test.TestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.isA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class EventusUIApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    GraphWriter graphWriter;

    @BeforeEach
    void populateGraph() {
        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "Order", 3, 1, ModuleStatus.HEALTHY));
        model.addModule(new ModuleNode("inventory", "Inventory", 2, 0, ModuleStatus.WARNING));
        model.addEvent(new EventNode("io.eventus.spring.test.order.OrderPlaced", "OrderPlaced", "order"));
        model.addEdge(new EventEdge("e1", "io.eventus.spring.test.order.OrderPlaced", "order", null, EdgeType.PUBLISHES));
        model.addEdge(new EventEdge("e2", "io.eventus.spring.test.order.OrderPlaced", null, "inventory", EdgeType.LISTENS_TO));
        graphWriter.write(model);
    }

    @Test
    void graphEndpointReturnsValidResponse() throws Exception {
        mockMvc.perform(get("/eventus/api/graph"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.modules").isArray())
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.edges").isArray())
                .andExpect(jsonPath("$.publications").isArray());
    }

    @Test
    void graphEndpointIncludesModuleData() throws Exception {
        mockMvc.perform(get("/eventus/api/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[?(@.id=='order')].name").value("Order"))
                .andExpect(jsonPath("$.modules[?(@.id=='order')].status").value("HEALTHY"))
                .andExpect(jsonPath("$.modules[?(@.id=='inventory')].status").value("WARNING"));
    }

    @Test
    void graphEndpointIncludesEventData() throws Exception {
        mockMvc.perform(get("/eventus/api/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[?(@.name=='OrderPlaced')].publisherModuleId").value("order"));
    }
}
