# S21 — MCP Security: API Key Filter

## Goal
Add opt-in API key authentication to the MCP endpoint so the event topology graph cannot be queried by anyone who can reach the server. Disabled by default; enabled with a single property.

## Acceptance Criteria
- [ ] `EventusMcpSecurityFilter` checks `Authorization: Bearer <key>` or `X-Eventus-Key: <key>` header
- [ ] Returns `401 Unauthorized` with JSON body `{"error":"Unauthorized"}` when key is missing or wrong
- [ ] Filter only applies to `/mcp/**` paths
- [ ] Disabled unless `eventus.mcp.security.enabled=true`
- [ ] `@ConditionalOnProperty` — no filter bean registered unless enabled
- [ ] Integration tests cover: correct key → 200, wrong key → 401, missing key → 401, non-MCP path → unaffected
- [ ] README documents the configuration
- [ ] `mvn verify` passes

## Implementation

```java
// io.eventus.mcp.security/EventusMcpSecurityFilter.java
public class EventusMcpSecurityFilter extends OncePerRequestFilter {
    private final String expectedKey;

    public EventusMcpSecurityFilter(String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {

        String provided = extractKey(request);
        if (!expectedKey.equals(provided)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/mcp");
    }

    private String extractKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return request.getHeader("X-Eventus-Key");
    }
}
```

## Auto-Configuration

```java
// io.eventus.mcp.autoconfigure/EventusMcpSecurityAutoConfiguration.java
@Configuration
@ConditionalOnClass(EventusMcpServer.class)
@ConditionalOnProperty(prefix = "eventus.mcp.security", name = "enabled", havingValue = "true")
@AutoConfiguration
public class EventusMcpSecurityAutoConfiguration {

    @Bean
    public EventusMcpSecurityFilter eventusMcpSecurityFilter(
        @Value("${eventus.mcp.security.api-key}") String apiKey
    ) {
        return new EventusMcpSecurityFilter(apiKey);
    }

    @Bean
    public FilterRegistrationBean<EventusMcpSecurityFilter> mcpSecurityFilterRegistration(
        EventusMcpSecurityFilter filter
    ) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/mcp/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
```

## Configuration Properties

```properties
# Enable MCP endpoint security (disabled by default)
eventus.mcp.security.enabled=true

# API key — use an environment variable, never hardcode
eventus.mcp.security.api-key=${EVENTUS_MCP_KEY}
```

## Tests Required

```java
@SpringBootTest(webEnvironment = RANDOM_PORT,
    properties = {
        "eventus.mcp.security.enabled=true",
        "eventus.mcp.security.api-key=test-secret"
    })
@AutoConfigureMockMvc
class EventusMcpSecurityFilterTest {

    @Autowired MockMvc mockMvc;

    @Test
    void correctBearerToken_returns200() throws Exception {
        mockMvc.perform(get("/mcp/tools")
            .header("Authorization", "Bearer test-secret"))
            .andExpect(status().isOk());
    }

    @Test
    void correctXEventusKey_returns200() throws Exception {
        mockMvc.perform(get("/mcp/tools")
            .header("X-Eventus-Key", "test-secret"))
            .andExpect(status().isOk());
    }

    @Test
    void wrongKey_returns401() throws Exception {
        mockMvc.perform(get("/mcp/tools")
            .header("Authorization", "Bearer wrong-key"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void missingKey_returns401() throws Exception {
        mockMvc.perform(get("/mcp/tools"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void nonMcpPath_notFiltered() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }
}

@SpringBootTest(webEnvironment = RANDOM_PORT,
    properties = "eventus.mcp.security.enabled=false")
@AutoConfigureMockMvc
class EventusMcpSecurityDisabledTest {

    @Autowired MockMvc mockMvc;

    @Test
    void mcpAccessibleWithoutKey_whenDisabled() throws Exception {
        mockMvc.perform(get("/mcp/tools"))
            .andExpect(status().isOk());
    }
}
```

## README Section to Add

```markdown
## MCP Endpoint Security

The MCP endpoint exposes your full event topology to any caller. In non-localhost
environments, enable API key protection:

```properties
eventus.mcp.security.enabled=true
eventus.mcp.security.api-key=${EVENTUS_MCP_KEY}
```

Set `EVENTUS_MCP_KEY` as an environment variable — never hardcode the key.

Callers must include one of:
- `Authorization: Bearer <key>`
- `X-Eventus-Key: <key>`

If Spring Security is already in your project, you can alternatively protect
`/mcp/**` via your existing `SecurityFilterChain` and leave
`eventus.mcp.security.enabled=false`.
```

## Done When
- Filter rejects bad/missing keys with `401` on all `/mcp/**` paths
- Non-MCP paths unaffected
- Security is off by default (`matchIfMissing=false` on the property)
- README documents how to configure and how to use Spring Security as an alternative
- `mvn verify` green
