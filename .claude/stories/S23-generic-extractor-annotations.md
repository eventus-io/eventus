# S23 — Generic JVM Extractor: Annotations in eventus-core

## Goal
Add three lightweight runtime annotations to `eventus-core` — `@EventModule`, `@Publishes`, and `@Listens` — that allow any JVM application to declare its event topology without a Spring or Modulith dependency.

## Acceptance Criteria
- [ ] `@EventModule`, `@Publishes`, and `@Listens` exist in `io.eventus.core.annotation`
- [ ] All three annotations have `@Retention(RUNTIME)` and correct `@Target`
- [ ] Annotations have zero framework dependencies (plain `java.*` only)
- [ ] Javadoc on each annotation explains its purpose and usage
- [ ] Unit tests verify annotation metadata is readable at runtime via reflection
- [ ] `mvn verify -pl eventus-core` passes

## Annotation Definitions

```java
// io.eventus.core.annotation/EventModule.java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface EventModule {
    /**
     * Logical module name used as the module ID in the Eventus graph.
     * Defaults to the simple class name if left blank.
     */
    String name() default "";
}
```

```java
// io.eventus.core.annotation/Publishes.java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Publishes {
    /**
     * The event classes published by this method.
     * Each class maps to an EventNode in the Eventus graph.
     */
    Class<?>[] value();
}
```

```java
// io.eventus.core.annotation/Listens.java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Listens {
    /**
     * The event classes consumed by this method.
     * Creates a LISTENS_TO edge from this module to each event's publisher.
     */
    Class<?>[] value();
}
```

## Tests Required

```java
// eventus-core/src/test/java/io/eventus/core/annotation/EventModuleAnnotationTest.java
class EventModuleAnnotationTest {

    @EventModule(name = "orders")
    static class OrderModule {}

    @Test
    void nameAttributeReadableAtRuntime() {
        EventModule annotation = OrderModule.class.getAnnotation(EventModule.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("orders");
    }

    @Test
    void defaultNameIsEmpty() {
        @EventModule
        class Unnamed {}
        assertThat(Unnamed.class.getAnnotation(EventModule.class).name()).isEmpty();
    }
}

class PublishesAnnotationTest {

    static class OrderService {
        @Publishes({OrderPlaced.class, OrderCancelled.class})
        public void placeOrder() {}
    }

    @Test
    void publishedTypesReadableAtRuntime() throws NoSuchMethodException {
        var method = OrderService.class.getMethod("placeOrder");
        Publishes pub = method.getAnnotation(Publishes.class);
        assertThat(pub.value()).containsExactly(OrderPlaced.class, OrderCancelled.class);
    }
}

class ListensAnnotationTest {

    static class InventoryService {
        @Listens(OrderPlaced.class)
        public void onOrderPlaced(OrderPlaced event) {}
    }

    @Test
    void listenedTypesReadableAtRuntime() throws NoSuchMethodException {
        var method = InventoryService.class.getMethod("onOrderPlaced", OrderPlaced.class);
        Listens listens = method.getAnnotation(Listens.class);
        assertThat(listens.value()).containsExactly(OrderPlaced.class);
    }
}

// Dummy event classes for tests
record OrderPlaced(String orderId) {}
record OrderCancelled(String orderId) {}
```

## Done When
- Three annotations exist in `io.eventus.core.annotation`
- All have `RUNTIME` retention
- No non-JDK imports anywhere in the annotation files
- Reflection tests confirm annotations are readable at runtime
- `mvn verify -pl eventus-core` green
