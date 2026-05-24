package io.eventus.spring.autoconfigure;

import io.eventus.core.memory.InMemoryGraphReader;
import io.eventus.core.memory.InMemoryGraphWriter;
import io.eventus.spring.actuator.EventusEventsEndpoint;
import io.eventus.spring.actuator.EventusModulesEndpoint;
import io.eventus.spring.actuator.EventusPublicationsEndpoint;
import io.eventus.spring.ui.EventusUIApiController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class EventusAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventusAutoConfiguration.class));

    @Test
    void autoConfigurationCreatesAllBeans() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(InMemoryGraphWriter.class);
            assertThat(ctx).hasSingleBean(InMemoryGraphReader.class);
            assertThat(ctx).hasSingleBean(EventusModulesEndpoint.class);
            assertThat(ctx).hasSingleBean(EventusEventsEndpoint.class);
            assertThat(ctx).hasSingleBean(EventusPublicationsEndpoint.class);
            assertThat(ctx).hasSingleBean(EventusUIApiController.class);
        });
    }

    @Test
    void autoConfigurationBacksOffWhenDisabled() {
        contextRunner
                .withPropertyValues("eventus.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(EventusModulesEndpoint.class);
                    assertThat(ctx).doesNotHaveBean(EventusEventsEndpoint.class);
                    assertThat(ctx).doesNotHaveBean(EventusPublicationsEndpoint.class);
                });
    }

    @Test
    void defaultPropertiesAreApplied() {
        contextRunner.run(ctx -> {
            var props = ctx.getBean(EventusProperties.class);
            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getPublications().getStaleThreshold())
                    .hasHours(2);
        });
    }

    @Test
    void customStaleThresholdIsRespected() {
        contextRunner
                .withPropertyValues("eventus.publications.stale-threshold=PT30M")
                .run(ctx -> {
                    var props = ctx.getBean(EventusProperties.class);
                    assertThat(props.getPublications().getStaleThreshold())
                            .hasMinutes(30);
                });
    }
}
