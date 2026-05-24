package io.eventus.mcp.autoconfigure;

import io.eventus.core.GraphReader;
import io.eventus.mcp.EventusGraphTools;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class EventusMcpAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventusMcpAutoConfiguration.class));

    @Test
    void mcpToolsBeanCreatedWhenGraphReaderPresent() {
        contextRunner
                .withBean(GraphReader.class, () -> Mockito.mock(GraphReader.class))
                .run(ctx -> assertThat(ctx).hasSingleBean(EventusGraphTools.class));
    }

    @Test
    void mcpToolsBeanNotCreatedWhenDisabled() {
        contextRunner
                .withPropertyValues("eventus.mcp.enabled=false")
                .withBean(GraphReader.class, () -> Mockito.mock(GraphReader.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(EventusGraphTools.class));
    }

    @Test
    void mcpToolsBeanNotCreatedWhenNoGraphReader() {
        contextRunner.run(ctx -> assertThat(ctx).doesNotHaveBean(EventusGraphTools.class));
    }

    @Test
    void propertiesDefaultsAreApplied() {
        contextRunner
                .withBean(GraphReader.class, () -> Mockito.mock(GraphReader.class))
                .run(ctx -> {
                    var props = ctx.getBean(EventusMcpProperties.class);
                    assertThat(props.isEnabled()).isTrue();
                    assertThat(props.getServerName()).isEqualTo("eventus");
                    assertThat(props.getServerVersion()).isEqualTo("1.0.0");
                });
    }
}
