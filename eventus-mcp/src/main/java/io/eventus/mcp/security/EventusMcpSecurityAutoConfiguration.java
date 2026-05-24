package io.eventus.mcp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@ConditionalOnProperty(prefix = "eventus.mcp.security", name = "enabled", havingValue = "true")
public class EventusMcpSecurityAutoConfiguration {

    @Bean
    public EventusMcpSecurityFilter eventusMcpSecurityFilter(
            @Value("${eventus.mcp.security.api-key}") String apiKey) {
        return new EventusMcpSecurityFilter(apiKey);
    }

    @Bean
    public FilterRegistrationBean<EventusMcpSecurityFilter> mcpSecurityFilterRegistration(
            EventusMcpSecurityFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/mcp/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
