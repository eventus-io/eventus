package io.eventus.spring.metrics;

import io.eventus.core.GraphReader;
import io.eventus.spring.test.TestApplication;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class)
class EventusMetricsCollectorTest {

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    GraphReader graphReader;

    @Test
    void allMetricsAreRegistered() {
        assertThat(meterRegistry.find("eventus.modules.total").gauge()).isNotNull();
        assertThat(meterRegistry.find("eventus.modules.healthy").gauge()).isNotNull();
        assertThat(meterRegistry.find("eventus.modules.warning").gauge()).isNotNull();
        assertThat(meterRegistry.find("eventus.modules.error").gauge()).isNotNull();
        assertThat(meterRegistry.find("eventus.events.total").gauge()).isNotNull();
        assertThat(meterRegistry.find("eventus.publications.completed").gauge()).isNotNull();
        assertThat(meterRegistry.find("eventus.publications.incomplete").gauge()).isNotNull();
        assertThat(meterRegistry.find("eventus.publications.stale").gauge()).isNotNull();
    }

    @Test
    void moduleTotalMetricReflectsCurrentCount() {
        Gauge gauge = meterRegistry.find("eventus.modules.total").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(graphReader.getModules().size());
    }

    @Test
    void publicationIncompleteMetricReflectsCurrentCount() {
        Gauge gauge = meterRegistry.find("eventus.publications.incomplete").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(
                graphReader.getPublications().stream()
                        .filter(p -> p.status() == io.eventus.core.model.PublicationStatus.INCOMPLETE)
                        .count()
        );
    }
}
