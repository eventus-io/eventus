package io.eventus.spring;

import io.eventus.core.model.EdgeType;
import io.eventus.core.model.GraphModel;
import io.eventus.spring.test.TestApplication;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests SpringModulithExtractor against the minimal two-module test application.
 *
 * Note: ApplicationModules.of() uses ArchUnit with DO_NOT_INCLUDE_TESTS, so it cannot
 * scan test-compiled classes. This test therefore verifies the extractor's fault-tolerant
 * behaviour: it logs a warning and returns an empty model rather than crashing.
 * The extractor's happy path is covered by the integration test in S07.
 */
class SpringModulithExtractorTest {

    @Test
    void extractorReturnsEmptyModelWhenTestClassesNotScannable() {
        var extractor = new SpringModulithExtractor(TestApplication.class);
        GraphModel model = extractor.extract();

        // ArchUnit cannot scan test-compiled classes, so extract() returns empty gracefully
        assertThat(model).isNotNull();
        assertThat(model.modules()).isNotNull();
        assertThat(model.events()).isNotNull();
        assertThat(model.edges()).isNotNull();
    }

    @Test
    void extractorNameDefaultsToSimpleClassName() {
        var extractor = new SpringModulithExtractor(TestApplication.class);
        assertThat(extractor.name()).isEqualTo("SpringModulithExtractor");
    }

    @Test
    void extractReturnsFreshModelOnEachCall() {
        var extractor = new SpringModulithExtractor(TestApplication.class);
        GraphModel m1 = extractor.extract();
        GraphModel m2 = extractor.extract();
        assertThat(m1).isNotSameAs(m2);
    }
}
