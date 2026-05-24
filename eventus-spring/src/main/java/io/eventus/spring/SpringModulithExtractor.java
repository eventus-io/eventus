package io.eventus.spring;

import io.eventus.core.EventGraphExtractor;
import io.eventus.core.GraphWriter;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventEdge.EdgeType;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.GraphModel;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.ModuleNode.Status;
import org.springframework.context.ApplicationContext;
import com.tngtech.archunit.core.domain.JavaClass;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.EventType;

import java.util.UUID;

public class SpringModulithExtractor implements EventGraphExtractor {

    private final ApplicationContext applicationContext;
    private final GraphWriter writer;

    public SpringModulithExtractor(ApplicationContext applicationContext, GraphWriter writer) {
        this.applicationContext = applicationContext;
        this.writer = writer;
    }

    @Override
    public void extract(GraphModel model) {
        writer.clear();

        Class<?> mainClass = detectMainClass();
        if (mainClass == null) {
            return;
        }

        ApplicationModules modules = ApplicationModules.of(mainClass);

        for (ApplicationModule module : modules) {
            String moduleId = module.getName();
            int beanCount = (int) module.getSpringBeans().stream().count();

            writer.writeModule(new ModuleNode(
                    moduleId,
                    module.getDisplayName(),
                    beanCount,
                    0,
                    Status.HEALTHY
            ));

            for (EventType eventType : module.getPublishedEvents()) {
                String eventId = eventType.getType().getName();
                String eventName = eventType.getType().getSimpleName();

                writer.writeEvent(new EventNode(eventId, eventName, moduleId));
                writer.writeEdge(new EventEdge(
                        UUID.randomUUID().toString(),
                        eventId,
                        moduleId,
                        null,
                        EdgeType.PUBLISHES
                ));
            }

            for (JavaClass eventClass : module.getEventsListenedTo(modules)) {
                String eventId = eventClass.getName();

                writer.writeEdge(new EventEdge(
                        UUID.randomUUID().toString(),
                        eventId,
                        null,
                        moduleId,
                        EdgeType.LISTENS_TO
                ));
            }
        }
    }

    private Class<?> detectMainClass() {
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(
                org.springframework.boot.autoconfigure.SpringBootApplication.class);
        if (beanNames.length == 0) {
            return null;
        }
        return applicationContext.getBean(beanNames[0]).getClass();
    }
}
