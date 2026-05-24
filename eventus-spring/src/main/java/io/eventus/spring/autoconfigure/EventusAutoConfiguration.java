package io.eventus.spring.autoconfigure;

import io.eventus.core.GraphReader;
import io.eventus.core.GraphWriter;
import io.eventus.core.memory.InMemoryGraph;
import io.eventus.core.memory.InMemoryGraphReader;
import io.eventus.core.memory.InMemoryGraphWriter;
import io.eventus.spring.SpringModulithExtractor;
import io.eventus.spring.actuator.EventusEventsEndpoint;
import io.eventus.spring.actuator.EventusModulesEndpoint;
import io.eventus.spring.actuator.EventusPublicationsEndpoint;
import io.eventus.spring.ui.EventusUIApiController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.core.ApplicationModules;

@AutoConfiguration
@ConditionalOnClass(ApplicationModules.class)
@ConditionalOnProperty(prefix = "eventus", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(EventusProperties.class)
public class EventusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InMemoryGraphWriter inMemoryGraphWriter() {
        return InMemoryGraph.writer();
    }

    @Bean
    @ConditionalOnMissingBean
    public InMemoryGraphReader inMemoryGraphReader(InMemoryGraphWriter writer) {
        return InMemoryGraph.reader(writer);
    }

    @Bean
    @ConditionalOnMissingBean(GraphWriter.class)
    public GraphWriter graphWriter(InMemoryGraphWriter writer) {
        return writer;
    }

    @Bean
    @ConditionalOnMissingBean(GraphReader.class)
    public GraphReader graphReader(InMemoryGraphReader reader) {
        return reader;
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringModulithExtractor springModulithExtractor(ApplicationContext ctx) {
        Class<?> mainClass = detectMainClass(ctx);
        return new SpringModulithExtractor(mainClass, ctx);
    }

    @Bean
    @ConditionalOnMissingBean(name = "eventusModulesEndpoint")
    public EventusModulesEndpoint eventusModulesEndpoint(GraphReader reader) {
        return new EventusModulesEndpoint(reader);
    }

    @Bean
    @ConditionalOnMissingBean(name = "eventusEventsEndpoint")
    public EventusEventsEndpoint eventusEventsEndpoint(GraphReader reader) {
        return new EventusEventsEndpoint(reader);
    }

    @Bean
    @ConditionalOnMissingBean(name = "eventusPublicationsEndpoint")
    public EventusPublicationsEndpoint eventusPublicationsEndpoint(GraphReader reader) {
        return new EventusPublicationsEndpoint(reader);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventusUIApiController eventusUIApiController(GraphReader reader) {
        return new EventusUIApiController(reader);
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> eventusExtractionTrigger(
            SpringModulithExtractor extractor, GraphWriter writer) {
        return event -> writer.write(extractor.extract());
    }

    private Class<?> detectMainClass(ApplicationContext ctx) {
        String[] names = ctx.getBeanNamesForAnnotation(
                org.springframework.boot.autoconfigure.SpringBootApplication.class);
        if (names.length > 0) {
            return ctx.getBean(names[0]).getClass();
        }
        return Object.class;
    }
}
