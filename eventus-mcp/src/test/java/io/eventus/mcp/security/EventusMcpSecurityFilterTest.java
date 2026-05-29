package io.eventus.mcp.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class EventusMcpSecurityFilterTest {

    private final EventusMcpSecurityFilter filter = new EventusMcpSecurityFilter("test-secret");

    @Test
    void correctBearerToken_allowsThrough() throws Exception {
        var request = new MockHttpServletRequest("GET", "/mcp/sse");
        request.addHeader("Authorization", "Bearer test-secret");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void correctXEventusKey_allowsThrough() throws Exception {
        var request = new MockHttpServletRequest("GET", "/mcp/sse");
        request.addHeader("X-Eventus-Key", "test-secret");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void wrongKey_returns401() throws Exception {
        var request = new MockHttpServletRequest("GET", "/mcp/sse");
        request.addHeader("Authorization", "Bearer wrong");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Unauthorized");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void missingKey_returns401() throws Exception {
        var request = new MockHttpServletRequest("GET", "/mcp/sse");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void nonMcpPath_skipsFilter() throws Exception {
        var request = new MockHttpServletRequest("GET", "/actuator/health");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // shouldNotFilter returns true → chain proceeds, status stays 200
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void emptyBearerToken_returns401() throws Exception {
        var request = new MockHttpServletRequest("GET", "/mcp/sse");
        request.addHeader("Authorization", "Bearer ");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void malformedBearerHeader_returns401() throws Exception {
        var request = new MockHttpServletRequest("GET", "/mcp/sse");
        request.addHeader("Authorization", "Bearertest-secret");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void emptyXEventusKey_returns401() throws Exception {
        var request = new MockHttpServletRequest("GET", "/mcp/sse");
        request.addHeader("X-Eventus-Key", "");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }
}
