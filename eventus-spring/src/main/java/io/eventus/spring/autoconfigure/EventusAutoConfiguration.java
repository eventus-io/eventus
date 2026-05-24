package io.eventus.spring.autoconfigure;

import io.eventus.core.InMemoryGraphReader;
import io.eventus.core.InMemoryGraphWriter;
import io.eventus.core.model.GraphModel;
import io.eventus.spring.SpringModulithExtractor;
import io.eventus.spring.actuator.EventusEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.core.ApplicationModules;

@AutoConfiguration
@ConditionalOnClass(ApplicationModules.class)
public class EventusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InMemoryGraphWriter inMemoryGraphWriter() {
        return new InMemoryGraphWriter();
    }

    @Bean
    @ConditionalOnMissingBean
    public InMemoryGraphReader inMemoryGraphReader(InMemoryGraphWriter writer) {
        return new InMemoryGraphReader(writer);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringModulithExtractor springModulithExtractor(ApplicationContext applicationContext,
                                                           InMemoryGraphWriter writer) {
        return new SpringModulithExtractor(applicationContext, writer);
    }

    @Bean
    @ConditionalOnAvailableEndpoint
    public EventusEndpoint eventusEndpoint(InMemoryGraphReader reader) {
        return new EventusEndpoint(reader);
    }

    @Bean
    public EventusExtractionListener eventusExtractionListener(SpringModulithExtractor extractor) {
        return new EventusExtractionListener(extractor);
    }

    static class EventusExtractionListener {
        private static final org.slf4j.Logger log =
                org.slf4j.LoggerFactory.getLogger(EventusExtractionListener.class);

        private final SpringModulithExtractor extractor;

        EventusExtractionListener(SpringModulithExtractor extractor) {
            this.extractor = extractor;
        }

        @EventListener(ApplicationReadyEvent.class)
        public void onApplicationReady() {
            try {
                extractor.extract(GraphModel.empty());
            } catch (Exception ex) {
                log.warn("Eventus graph extraction failed — topology data will be unavailable. Cause: {}", ex.getMessage());
            }
        }
    }
}
