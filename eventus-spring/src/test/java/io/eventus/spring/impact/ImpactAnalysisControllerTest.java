package io.eventus.spring.impact;

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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ImpactAnalysisControllerTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @Autowired
    GraphWriter graphWriter;

    @BeforeEach
    void populateGraph() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "Order", 3, 0, ModuleStatus.HEALTHY));
        model.addModule(new ModuleNode("inventory", "Inventory", 2, 0, ModuleStatus.HEALTHY));
        model.addEvent(new EventNode("io.eventus.spring.test.order.OrderPlaced", "OrderPlaced", "order"));
        model.addEdge(new EventEdge("e1", "io.eventus.spring.test.order.OrderPlaced", "order", null, EdgeType.PUBLISHES));
        model.addEdge(new EventEdge("e2", "io.eventus.spring.test.order.OrderPlaced", null, "inventory", EdgeType.LISTENS_TO));
        graphWriter.write(model);
    }

    @Test
    void eventImpactEndpointReturnsAffectedModules() throws Exception {
        mockMvc.perform(get("/eventus/api/impact/event/io.eventus.spring.test.order.OrderPlaced"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedModules", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.directListeners").value(1))
                .andExpect(jsonPath("$.publisherModuleId").value("order"));
    }

    @Test
    void eventImpactEndpointReturns404ForUnknownEvent() throws Exception {
        mockMvc.perform(get("/eventus/api/impact/event/com.example.UnknownEvent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void moduleImpactEndpointReturnsPublishedEvents() throws Exception {
        mockMvc.perform(get("/eventus/api/impact/module/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedEvents").isArray())
                .andExpect(jsonPath("$.totalAffectedModules").value(1));
    }

    @Test
    void moduleImpactEndpointReturns404ForUnknownModule() throws Exception {
        mockMvc.perform(get("/eventus/api/impact/module/unknown"))
                .andExpect(status().isNotFound());
    }
}
