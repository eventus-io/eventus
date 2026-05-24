package io.eventus.core.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class EventusAnnotationsTest {

    record OrderPlaced(String id) {}
    record OrderCancelled(String id) {}

    @EventModule(name = "orders")
    static class OrderModule {
        @Publishes({OrderPlaced.class, OrderCancelled.class})
        public void placeOrder() {}
    }

    @EventModule
    static class Unnamed {}

    static class InventoryModule {
        @Listens(OrderPlaced.class)
        public void onOrderPlaced(OrderPlaced event) {}
    }

    @Test
    void eventModule_nameReadableAtRuntime() {
        var ann = OrderModule.class.getAnnotation(EventModule.class);
        assertThat(ann).isNotNull();
        assertThat(ann.name()).isEqualTo("orders");
    }

    @Test
    void eventModule_defaultNameIsBlank() {
        var ann = Unnamed.class.getAnnotation(EventModule.class);
        assertThat(ann.name()).isEmpty();
    }

    @Test
    void publishes_typesReadableAtRuntime() throws NoSuchMethodException {
        Method method = OrderModule.class.getMethod("placeOrder");
        var pub = method.getAnnotation(Publishes.class);
        assertThat(pub).isNotNull();
        assertThat(pub.value()).containsExactly(OrderPlaced.class, OrderCancelled.class);
    }

    @Test
    void listens_typesReadableAtRuntime() throws NoSuchMethodException {
        Method method = InventoryModule.class.getMethod("onOrderPlaced", OrderPlaced.class);
        var listens = method.getAnnotation(Listens.class);
        assertThat(listens).isNotNull();
        assertThat(listens.value()).containsExactly(OrderPlaced.class);
    }

    @Test
    void annotations_haveNoNonJdkDependencies() {
        for (var imported : EventModule.class.getDeclaredClasses()) {
            assertThat(imported.getPackageName()).startsWith("java.");
        }
    }
}
