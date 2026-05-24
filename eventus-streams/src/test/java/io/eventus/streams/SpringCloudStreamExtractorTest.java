package io.eventus.streams;

import io.eventus.core.model.EdgeType;
import io.eventus.core.model.ModuleNode;
import io.eventus.core.model.EventNode;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class SpringCloudStreamExtractorTest {

    @Test
    void extractsServiceAsModuleNode() {
        var env = buildEnv("order-service");
        var extractor = new SpringCloudStreamExtractor(env);
        var model = extractor.extract();

        assertThat(model.modules()).hasSize(1);
        assertThat(model.modules().get(0).id()).isEqualTo("order-service");
        assertThat(model.modules().get(0).name()).isEqualTo("order-service");
    }

    @Test
    void extractsBindingsAsEvents() {
        var env = buildEnv("order-service");
        var extractor = new SpringCloudStreamExtractor(env);
        var model = extractor.extract();

        assertThat(model.events())
                .extracting(EventNode::name)
                .containsExactlyInAnyOrder("orders-topic", "inventory-topic");
    }

    @Test
    void createsPublishesEdgeForOutputBinding() {
        var env = buildEnv("order-service");
        var extractor = new SpringCloudStreamExtractor(env);
        var model = extractor.extract();

        assertThat(model.edges())
                .anyMatch(e -> e.edgeType() == EdgeType.PUBLISHES
                        && "order-service".equals(e.fromModuleId()));
    }

    @Test
    void createsListensToEdgeForInputBinding() {
        var env = buildEnv("order-service");
        var extractor = new SpringCloudStreamExtractor(env);
        var model = extractor.extract();

        assertThat(model.edges())
                .anyMatch(e -> e.edgeType() == EdgeType.LISTENS_TO
                        && "order-service".equals(e.toModuleId()));
    }

    @Test
    void returnsEmptyModelWhenAppNameNotSet() {
        var env = new StandardEnvironment();
        var extractor = new SpringCloudStreamExtractor(env);
        var model = extractor.extract();

        assertThat(model.modules()).isEmpty();
    }

    @Test
    void returnsEmptyModelWhenNoBindings() {
        var env = new MockEnvironment().withProperty("spring.application.name", "my-service");
        var extractor = new SpringCloudStreamExtractor(env);
        var model = extractor.extract();

        assertThat(model.modules()).isEmpty();
    }

    private static MockEnvironment buildEnv(String appName) {
        return new MockEnvironment()
                .withProperty("spring.application.name", appName)
                .withProperty("spring.cloud.stream.bindings.orders-out.destination", "orders-topic")
                .withProperty("spring.cloud.stream.bindings.inventory-in.destination", "inventory-topic")
                .withProperty("spring.cloud.stream.bindings.inventory-in.group", appName);
    }
}
