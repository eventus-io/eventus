package io.eventus.core;

import io.eventus.core.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraphModelTest {

    @Test
    void addingNodesMakesThemRetrievable() {
        var model = new GraphModel();
        model.addModule(new ModuleNode("m1", "Order", 3, 1, ModuleStatus.HEALTHY));
        model.addEvent(new EventNode("com.example.OrderPlaced", "OrderPlaced", "m1"));
        model.addEdge(new EventEdge("e1", "com.example.OrderPlaced", "m1", null, EdgeType.PUBLISHES));
        model.addPublication(new PublicationRecord("p1", "com.example.OrderPlaced",
                "InventoryService.on", "m2", PublicationStatus.INCOMPLETE, Instant.now()));

        assertThat(model.modules()).hasSize(1);
        assertThat(model.events()).hasSize(1);
        assertThat(model.edges()).hasSize(1);
        assertThat(model.publications()).hasSize(1);
    }

    @Test
    void modulesListIsUnmodifiable() {
        var model = new GraphModel();
        model.addModule(new ModuleNode("m1", "Order", 1, 0, ModuleStatus.HEALTHY));

        assertThatThrownBy(() -> model.modules().add(new ModuleNode("m2", "X", 0, 0, ModuleStatus.HEALTHY)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void eventsListIsUnmodifiable() {
        var model = new GraphModel();
        assertThatThrownBy(() -> model.events().add(new EventNode("id", "Name", "m1")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void edgesListIsUnmodifiable() {
        var model = new GraphModel();
        assertThatThrownBy(() -> model.edges().add(new EventEdge("id", "e1", "m1", null, EdgeType.PUBLISHES)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void publicationsListIsUnmodifiable() {
        var model = new GraphModel();
        assertThatThrownBy(() -> model.publications().add(
                new PublicationRecord("id", "type", "listener", "m1", PublicationStatus.COMPLETED, Instant.now())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void freshModelHasEmptyLists() {
        var model = new GraphModel();
        assertThat(model.modules()).isEmpty();
        assertThat(model.events()).isEmpty();
        assertThat(model.edges()).isEmpty();
        assertThat(model.publications()).isEmpty();
    }
}
