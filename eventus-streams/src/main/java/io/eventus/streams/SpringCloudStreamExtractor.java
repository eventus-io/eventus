package io.eventus.streams;

import io.eventus.core.EventGraphExtractor;
import io.eventus.core.model.*;
import io.eventus.streams.config.KafkaBindingReader;
import io.eventus.streams.model.BindingType;
import io.eventus.streams.model.StreamBinding;
import io.eventus.streams.model.StreamTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.UUID;

public class SpringCloudStreamExtractor implements EventGraphExtractor {

    private static final Logger log = LoggerFactory.getLogger(SpringCloudStreamExtractor.class);

    private final KafkaBindingReader bindingReader;
    private final String applicationName;

    public SpringCloudStreamExtractor(ConfigurableEnvironment environment) {
        this.bindingReader = new KafkaBindingReader(environment);
        this.applicationName = environment.getProperty("spring.application.name", "unknown");
    }

    @Override
    public GraphModel extract() {
        var model = new GraphModel();

        if ("unknown".equals(applicationName)) {
            log.warn("Eventus Streams: spring.application.name not set — skipping extraction");
            return model;
        }

        try {
            StreamTopology topology = bindingReader.readTopology(applicationName);

            if (topology.bindings().isEmpty()) {
                log.warn("Eventus Streams: no spring.cloud.stream.bindings.* found for '{}'", applicationName);
                return model;
            }

            model.addModule(new ModuleNode(applicationName, applicationName, 0, 0, ModuleStatus.HEALTHY));

            for (StreamBinding binding : topology.bindings()) {
                String eventId = binding.group() != null
                        ? binding.topic() + ":" + binding.group()
                        : binding.topic();

                String publisherModule = binding.type() == BindingType.OUTPUT ? applicationName : "external";

                if (model.events().stream().noneMatch(e -> e.id().equals(eventId))) {
                    model.addEvent(new EventNode(eventId, binding.topic(), publisherModule));
                }

                if (binding.type() == BindingType.OUTPUT) {
                    model.addEdge(new EventEdge(UUID.randomUUID().toString(),
                            eventId, applicationName, null, EdgeType.PUBLISHES));
                } else {
                    model.addEdge(new EventEdge(UUID.randomUUID().toString(),
                            eventId, null, applicationName, EdgeType.LISTENS_TO));
                }
            }

        } catch (Exception e) {
            log.warn("Eventus Streams: failed to extract topology for '{}' — {}", applicationName, e.getMessage());
        }

        return model;
    }
}
