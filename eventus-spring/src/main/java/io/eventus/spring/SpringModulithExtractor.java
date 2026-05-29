package io.eventus.spring;

import com.tngtech.archunit.core.domain.JavaClass;
import io.eventus.core.EventGraphExtractor;
import io.eventus.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.EventType;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SpringModulithExtractor implements EventGraphExtractor {

    private static final Logger log = LoggerFactory.getLogger(SpringModulithExtractor.class);

    private final Class<?> applicationClass;
    private final ApplicationContext applicationContext;

    public SpringModulithExtractor(Class<?> applicationClass, ApplicationContext applicationContext) {
        this.applicationClass = applicationClass;
        this.applicationContext = applicationContext;
    }

    public SpringModulithExtractor(Class<?> applicationClass) {
        this(applicationClass, null);
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

        // Build module ID → base package mapping for event attribution
        Map<String, String> moduleIdByPackage = new LinkedHashMap<>();

        for (ApplicationModule module : modules) {
            String moduleId = module.getName();
            int beanCount = (int) module.getSpringBeans().stream().count();
            model.addModule(new ModuleNode(moduleId, module.getDisplayName(), beanCount, 0, ModuleStatus.HEALTHY));

            String basePackage = applicationClass.getPackageName() + "." + moduleId;
            moduleIdByPackage.put(basePackage, moduleId);

            // Primary path: ArchUnit-based event detection (works on Java ≤ 22)
            for (EventType eventType : module.getPublishedEvents()) {
                String eventId = eventType.getType().getName();
                model.addEvent(new EventNode(eventId, eventType.getType().getSimpleName(), moduleId));
                model.addEdge(new EventEdge(edgeId(eventId, moduleId, null, EdgeType.PUBLISHES), eventId, moduleId, null, EdgeType.PUBLISHES));
            }
            for (JavaClass eventClass : module.getEventsListenedTo(modules)) {
                model.addEdge(new EventEdge(edgeId(eventClass.getName(), null, moduleId, EdgeType.LISTENS_TO), eventClass.getName(), null, moduleId, EdgeType.LISTENS_TO));
            }
        }

        // Fallback: reflection-based detection via @ApplicationModuleListener.
        // Used when ArchUnit cannot parse JDK class files on newer JVMs (> Java 22).
        if (model.events().isEmpty() && applicationContext != null) {
            log.debug("Eventus: ArchUnit event detection returned no results, falling back to @ApplicationModuleListener scan");
            detectEventsFromListeners(model, moduleIdByPackage);
        }
    }

    private void detectEventsFromListeners(GraphModel model, Map<String, String> moduleIdByPackage) {
        Set<String> knownEventIds = new HashSet<>();

        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception e) {
                continue;
            }

            // Unwrap CGLIB/JDK proxies to get the actual class and its declared methods
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            String listeningModuleId = resolveModuleId(targetClass.getPackageName(), moduleIdByPackage);
            if (listeningModuleId == null) continue;

            for (Method method : targetClass.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.isAnnotated(method, ApplicationModuleListener.class)) continue;
                if (method.getParameterCount() != 1) continue;

                Class<?> eventType = method.getParameterTypes()[0];
                String eventId = eventType.getName();
                String publishingModuleId = resolveModuleId(eventType.getPackageName(), moduleIdByPackage);
                if (publishingModuleId == null) continue;

                if (knownEventIds.add(eventId)) {
                    model.addEvent(new EventNode(eventId, eventType.getSimpleName(), publishingModuleId));
                    model.addEdge(new EventEdge(edgeId(eventId, publishingModuleId, null, EdgeType.PUBLISHES), eventId, publishingModuleId, null, EdgeType.PUBLISHES));
                }
                model.addEdge(new EventEdge(edgeId(eventId, null, listeningModuleId, EdgeType.LISTENS_TO), eventId, null, listeningModuleId, EdgeType.LISTENS_TO));
            }
        }
    }

    private String resolveModuleId(String packageName, Map<String, String> moduleIdByPackage) {
        return moduleIdByPackage.entrySet().stream()
                .filter(e -> packageName.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static String edgeId(String eventId, String fromModuleId, String toModuleId, EdgeType type) {
        return eventId + ":" + type.name() + ":" + (fromModuleId != null ? fromModuleId : "") + ":" + (toModuleId != null ? toModuleId : "");
    }
}
