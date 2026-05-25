package io.eventus.mcp.autoconfigure;

import io.eventus.core.GraphReader;
import io.eventus.mcp.EventusGraphTools;
import io.eventus.spring.autoconfigure.EventusAutoConfiguration;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = EventusAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties")
@ConditionalOnProperty(prefix = "eventus.mcp", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(EventusMcpProperties.class)
public class EventusMcpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(GraphReader.class)
    public EventusGraphTools eventusGraphTools(GraphReader graphReader) {
        return new EventusGraphTools(graphReader);
    }

    @Bean(name = "eventusToolCallbackProvider")
    @ConditionalOnMissingBean(name = "eventusToolCallbackProvider")
    @ConditionalOnBean(EventusGraphTools.class)
    public ToolCallbackProvider eventusToolCallbackProvider(EventusGraphTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
