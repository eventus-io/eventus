package io.eventus.spring.violations;

import io.eventus.core.GraphWriter;
import io.eventus.core.model.*;
import io.eventus.spring.test.TestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
class ViolationsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    GraphWriter graphWriter;

    @BeforeEach
    void populateGraph() {
        var model = new GraphModel();
        model.addModule(new ModuleNode("order", "Order", 3, 0, ModuleStatus.HEALTHY));
        model.addModule(new ModuleNode("inventory", "Inventory", 2, 0, ModuleStatus.HEALTHY));
        model.addEvent(new EventNode("io.eventus.spring.test.order.OrderPlaced", "OrderPlaced", "order"));
        model.addEdge(new EventEdge("e1", "io.eventus.spring.test.order.OrderPlaced", "order", null, EdgeType.PUBLISHES));
        // inventory listens (hidden coupling = no DEPENDS_ON)
        model.addEdge(new EventEdge("e2", "io.eventus.spring.test.order.OrderPlaced", null, "inventory", EdgeType.LISTENS_TO));
        graphWriter.write(model);
    }

    @Test
    void violationsEndpointReturnsArray() throws Exception {
        mockMvc.perform(get("/eventus/api/violations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void violationsCanBeFilteredBySeverity() throws Exception {
        mockMvc.perform(get("/eventus/api/violations?severity=WARNING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].severity", everyItem(equalTo("WARNING"))));
    }

    @Test
    void violationsCanBeFilteredByType() throws Exception {
        mockMvc.perform(get("/eventus/api/violations?type=HIDDEN_COUPLING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type", everyItem(equalTo("HIDDEN_COUPLING"))));
    }

    @Test
    void hiddenCouplingViolationIsDetected() throws Exception {
        mockMvc.perform(get("/eventus/api/violations?type=HIDDEN_COUPLING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}
