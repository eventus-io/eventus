package io.eventus.spring;

import com.tngtech.archunit.core.domain.JavaClass;
import io.eventus.core.EventGraphExtractor;
import io.eventus.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.EventType;

import java.util.UUID;

public class SpringModulithExtractor implements EventGraphExtractor {

    private static final Logger log = LoggerFactory.getLogger(SpringModulithExtractor.class);

    private final Class<?> applicationClass;

    public SpringModulithExtractor(Class<?> applicationClass) {
        this.applicationClass = applicationClass;
    }

    @Override
    public GraphModel extract() {
        var model = new GraphModel();
        try {
            doExtract(model);
        } catch (Exception ex) {
            log.warn("Eventus: could not extract module model — {}", ex.getMessage());
        }
        return model;
    }

    private void doExtract(GraphModel model) {
        ApplicationModules modules = ApplicationModules.of(applicationClass);

        for (ApplicationModule module : modules) {
            String moduleId = module.getName();
            int beanCount = (int) module.getSpringBeans().stream().count();

            model.addModule(new ModuleNode(moduleId, module.getDisplayName(), beanCount, 0, ModuleStatus.HEALTHY));

            for (EventType eventType : module.getPublishedEvents()) {
                String eventId = eventType.getType().getName();
                String eventName = eventType.getType().getSimpleName();

                model.addEvent(new EventNode(eventId, eventName, moduleId));
                model.addEdge(new EventEdge(
                        UUID.randomUUID().toString(), eventId, moduleId, null, EdgeType.PUBLISHES));
            }

            for (JavaClass eventClass : module.getEventsListenedTo(modules)) {
                model.addEdge(new EventEdge(
                        UUID.randomUUID().toString(), eventClass.getName(), null, moduleId, EdgeType.LISTENS_TO));
            }
        }
    }
}
