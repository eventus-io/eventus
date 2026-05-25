package io.eventus.ui.chat;

import io.eventus.core.GraphReader;
import io.eventus.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = "eventus.chat.enabled=true")
class EventusChatControllerTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @Autowired
    EventusChatProperties chatProperties;

    @TestConfiguration
    static class TestConfig {
        @Bean
        GraphReader graphReader() {
            return new GraphReader() {
                @Override public List<ModuleNode> getModules() { return List.of(); }
                @Override public List<io.eventus.core.model.EventNode> getEvents() { return List.of(); }
                @Override public List<EventEdge> getEdges() { return List.of(); }
                @Override public List<EventEdge> getEdgesForEvent(String eventId) { return List.of(); }
                @Override public List<PublicationRecord> getPublications() { return List.of(); }
                @Override public List<PublicationRecord> getIncompletePublications() { return List.of(); }
            };
        }

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void returns400ForBlankQuestion() throws Exception {
        mockMvc.perform(post("/eventus/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns503WhenApiKeyNotConfigured() throws Exception {
        mockMvc.perform(post("/eventus/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"What modules exist?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("API key")));
    }
}
