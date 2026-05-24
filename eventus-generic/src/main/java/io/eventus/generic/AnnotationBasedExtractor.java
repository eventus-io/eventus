package io.eventus.generic;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.eventus.core.EventGraphExtractor;
import io.eventus.core.annotation.EventModule;
import io.eventus.core.annotation.Listens;
import io.eventus.core.annotation.Publishes;
import io.eventus.core.model.EdgeType;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.GraphModel;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.ModuleStatus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnnotationBasedExtractor implements EventGraphExtractor {

    private final List<String> basePackages;

    public AnnotationBasedExtractor(List<String> basePackages) {
        this.basePackages = List.copyOf(basePackages);
    }

    @Override
    public GraphModel extract() {
        GraphModel model = new GraphModel();
        Map<String, String> classToModuleId = new HashMap<>();
        Map<String, EventNode> events = new LinkedHashMap<>();
        List<EventEdge> edges = new ArrayList<>();

        try (ScanResult scan = new ClassGraph()
                .acceptPackages(basePackages.toArray(String[]::new))
                .enableAllInfo()
                .scan()) {

            for (var classInfo : scan.getClassesWithAnnotation(EventModule.class.getName())) {
                Class<?> clazz = classInfo.loadClass();
                EventModule ann = clazz.getAnnotation(EventModule.class);
                String moduleId = ann.name().isBlank() ? clazz.getSimpleName() : ann.name();
                classToModuleId.put(clazz.getName(), moduleId);
                model.addModule(new ModuleNode(moduleId, moduleId, 0, 0, ModuleStatus.HEALTHY));
            }

            for (var classInfo : scan.getClassesWithAnnotation(EventModule.class.getName())) {
                Class<?> clazz = classInfo.loadClass();
                String moduleId = classToModuleId.get(clazz.getName());

                for (Method method : clazz.getDeclaredMethods()) {
                    Publishes pub = method.getAnnotation(Publishes.class);
                    if (pub != null) {
                        for (Class<?> eventType : pub.value()) {
                            String eventId = eventType.getName();
                            events.putIfAbsent(eventId,
                                    new EventNode(eventId, eventType.getSimpleName(), moduleId));
                            edges.add(new EventEdge(
                                    moduleId + "_publishes_" + eventId,
                                    eventId, moduleId, null, EdgeType.PUBLISHES));
                        }
                    }

                    Listens listens = method.getAnnotation(Listens.class);
                    if (listens != null) {
                        for (Class<?> eventType : listens.value()) {
                            String eventId = eventType.getName();
                            edges.add(new EventEdge(
                                    moduleId + "_listens_" + eventId,
                                    eventId, null, moduleId, EdgeType.LISTENS_TO));
                        }
                    }
                }
            }
        }

        events.values().forEach(model::addEvent);
        edges.forEach(model::addEdge);
        return model;
    }
}
