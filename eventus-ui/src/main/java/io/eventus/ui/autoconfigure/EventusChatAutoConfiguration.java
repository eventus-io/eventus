package io.eventus.ui.autoconfigure;

import io.eventus.core.GraphReader;
import io.eventus.ui.chat.EventusChatController;
import io.eventus.ui.chat.EventusChatProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "eventus.chat", name = "enabled")
@EnableConfigurationProperties(EventusChatProperties.class)
public class EventusChatAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(GraphReader.class)
    public EventusChatController eventusChatController(GraphReader graphReader,
                                                        EventusChatProperties properties,
                                                        RestClient.Builder restClientBuilder) {
        return new EventusChatController(graphReader, properties, restClientBuilder);
    }
}
