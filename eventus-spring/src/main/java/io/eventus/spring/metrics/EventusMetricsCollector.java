package io.eventus.spring.metrics;

import io.eventus.core.GraphReader;
import io.eventus.core.model.ModuleStatus;
import io.eventus.core.model.PublicationStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

public class EventusMetricsCollector {

    public EventusMetricsCollector(MeterRegistry meterRegistry, GraphReader graphReader) {
        Gauge.builder("eventus.modules.total", graphReader,
                r -> r.getModules().size())
                .description("Total number of modules").register(meterRegistry);
        Gauge.builder("eventus.modules.healthy", graphReader,
                r -> r.getModules().stream().filter(m -> m.status() == ModuleStatus.HEALTHY).count())
                .description("Modules in HEALTHY status").register(meterRegistry);
        Gauge.builder("eventus.modules.warning", graphReader,
                r -> r.getModules().stream().filter(m -> m.status() == ModuleStatus.WARNING).count())
                .description("Modules in WARNING status").register(meterRegistry);
        Gauge.builder("eventus.modules.error", graphReader,
                r -> r.getModules().stream().filter(m -> m.status() == ModuleStatus.ERROR).count())
                .description("Modules in ERROR status").register(meterRegistry);
        Gauge.builder("eventus.events.total", graphReader,
                r -> r.getEvents().size())
                .description("Total number of domain events").register(meterRegistry);
        Gauge.builder("eventus.publications.completed", graphReader,
                r -> r.getPublications().stream().filter(p -> p.status() == PublicationStatus.COMPLETED).count())
                .description("Completed event publications").register(meterRegistry);
        Gauge.builder("eventus.publications.incomplete", graphReader,
                r -> r.getPublications().stream().filter(p -> p.status() == PublicationStatus.INCOMPLETE).count())
                .description("Incomplete event publications").register(meterRegistry);
        Gauge.builder("eventus.publications.stale", graphReader,
                r -> r.getPublications().stream().filter(p -> p.status() == PublicationStatus.STALE).count())
                .description("Stale event publications").register(meterRegistry);
    }
}
