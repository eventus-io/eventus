package io.eventus.generic;

import io.eventus.core.annotation.EventModule;
import io.eventus.core.annotation.Listens;
import io.eventus.core.annotation.Publishes;
import io.eventus.core.model.EdgeType;
import io.eventus.core.model.EventEdge;
import io.eventus.core.model.EventNode;
import io.eventus.core.model.GraphModel;
import io.eventus.core.model.ModuleNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationBasedExtractorTest {

    // --- test fixtures (inner classes so they're in the test package) ---

    record OrderPlaced(String id) {}
    record OrderCancelled(String id) {}

    @EventModule(name = "orders")
    static class OrderModule {
        @Publishes({OrderPlaced.class, OrderCancelled.class})
        public void placeOrder() {}

        @Publishes(OrderPlaced.class)
        public void reorder() {}  // same event type published by two methods
    }

    @EventModule(name = "inventory")
    static class InventoryModule {
        @Listens(OrderPlaced.class)
        public void onOrderPlaced(OrderPlaced e) {}
    }

    @EventModule
    static class UnnamedModule {}

    private final AnnotationBasedExtractor extractor =
            new AnnotationBasedExtractor(List.of("io.eventus.generic"));

    @Test
    void extractsModules() {
        GraphModel graph = extractor.extract();
        assertThat(graph.modules())
                .extracting(ModuleNode::id)
                .contains("orders", "inventory", "UnnamedModule");
    }

    @Test
    void unnamedModule_usesSimpleClassName() {
        GraphModel graph = extractor.extract();
        assertThat(graph.modules())
                .extracting(ModuleNode::name)
                .contains("UnnamedModule");
    }

    @Test
    void extractsEvents() {
        GraphModel graph = extractor.extract();
        assertThat(graph.events())
                .extracting(EventNode::name)
                .containsExactlyInAnyOrder("OrderPlaced", "OrderCancelled");
    }

    @Test
    void deduplicatesEventsPublishedByMultipleMethods() {
        GraphModel graph = extractor.extract();
        long orderPlacedCount = graph.events().stream()
                .filter(e -> e.name().equals("OrderPlaced"))
                .count();
        assertThat(orderPlacedCount).isEqualTo(1);
    }

    @Test
    void extractsPublishesEdge() {
        GraphModel graph = extractor.extract();
        assertThat(graph.edges())
                .filteredOn(e -> e.edgeType() == EdgeType.PUBLISHES)
                .extracting(EventEdge::fromModuleId)
                .contains("orders");
    }

    @Test
    void extractsListensEdge() {
        GraphModel graph = extractor.extract();
        assertThat(graph.edges())
                .filteredOn(e -> e.edgeType() == EdgeType.LISTENS_TO)
                .extracting(EventEdge::toModuleId)
                .contains("inventory");
    }

    @Test
    void noSpringContextRequired() {
        // AnnotationBasedExtractor must work with no Spring ApplicationContext
        GraphModel graph = new AnnotationBasedExtractor(List.of("io.eventus.generic")).extract();
        assertThat(graph).isNotNull();
    }

    @Test
    void emptyPackageList_returnsEmptyGraph() {
        GraphModel graph = new AnnotationBasedExtractor(List.of()).extract();
        assertThat(graph.modules()).isEmpty();
        assertThat(graph.events()).isEmpty();
        assertThat(graph.edges()).isEmpty();
    }

    @Test
    void unknownPackage_returnsEmptyGraph() {
        GraphModel graph = new AnnotationBasedExtractor(List.of("com.nonexistent.pkg")).extract();
        assertThat(graph.modules()).isEmpty();
        assertThat(graph.events()).isEmpty();
    }

    @Test
    void publisherModuleId_isSetCorrectly() {
        GraphModel graph = extractor.extract();
        assertThat(graph.events())
                .filteredOn(e -> e.name().equals("OrderPlaced"))
                .extracting(EventNode::publisherModuleId)
                .containsOnly("orders");
    }

    @Test
    void edgeIds_areDeterministicAcrossExtractions() {
        GraphModel first  = extractor.extract();
        GraphModel second = extractor.extract();
        var firstIds  = first.edges().stream().map(EventEdge::id).sorted().toList();
        var secondIds = second.edges().stream().map(EventEdge::id).sorted().toList();
        assertThat(firstIds).isEqualTo(secondIds);
    }
}
