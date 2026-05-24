package io.eventus.ui.chat;

import io.eventus.core.GraphReader;
import io.eventus.core.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/eventus/api")
public class EventusChatController {

    private final GraphReader graphReader;
    private final EventusChatProperties properties;
    private final RestClient restClient;

    public EventusChatController(GraphReader graphReader,
                                  EventusChatProperties properties,
                                  RestClient.Builder restClientBuilder) {
        this.graphReader = graphReader;
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl("https://api.anthropic.com")
                .build();
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (!StringUtils.hasText(request.question())) {
            return ResponseEntity.badRequest().build();
        }
        if (!StringUtils.hasText(properties.getAnthropicApiKey())) {
            return ResponseEntity.status(503)
                    .body(new ChatResponse(
                            "Anthropic API key not configured. Set eventus.chat.anthropic-api-key."));
        }
        String graphContext = buildGraphContext();
        String answer = callAnthropic(request.question(), graphContext);
        return ResponseEntity.ok(new ChatResponse(answer));
    }

    private String buildGraphContext() {
        var sb = new StringBuilder();
        sb.append("Modules:\n");
        for (ModuleNode m : graphReader.getModules()) {
            sb.append("  - ").append(m.name())
              .append(" (").append(m.status()).append(", ")
              .append(m.beanCount()).append(" beans)\n");
        }
        sb.append("Events:\n");
        for (EventNode e : graphReader.getEvents()) {
            sb.append("  - ").append(e.name())
              .append(" published by ").append(e.publisherModuleId()).append("\n");
            List<EventEdge> listeners = graphReader.getEdgesForEvent(e.id()).stream()
                    .filter(edge -> edge.edgeType() == EdgeType.LISTENS_TO)
                    .toList();
            for (EventEdge edge : listeners) {
                sb.append("    consumed by ").append(edge.toModuleId()).append("\n");
            }
        }
        List<PublicationRecord> incomplete = graphReader.getIncompletePublications();
        if (!incomplete.isEmpty()) {
            sb.append("Incomplete publications:\n");
            for (PublicationRecord p : incomplete) {
                sb.append("  - ").append(p.listenerName())
                  .append(" (").append(p.status()).append(", since ").append(p.publishedAt()).append(")\n");
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String callAnthropic(String question, String context) {
        String systemPrompt = "You are an expert on this Spring Modulith application's event topology. " +
                "Answer ONLY from the context below. Be concise.\n\n" + context;

        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "max_tokens", properties.getMaxTokens(),
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", question)));

        Map<String, Object> response = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", properties.getAnthropicApiKey())
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response == null) return "No response from AI.";
        var content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) return "Empty response from AI.";
        Object text = content.getFirst().get("text");
        return text != null ? text.toString() : "No text in response.";
    }
}
