# S03 — In-Memory Backend

## Goal
Implement `InMemoryGraphWriter` and `InMemoryGraphReader` in `eventus-core`.
This is the default backend — zero infrastructure, works in dev and test.

## Acceptance Criteria
- [ ] `InMemoryGraphWriter` implements `GraphWriter`
- [ ] `InMemoryGraphReader` implements `GraphReader`
- [ ] Both share the same underlying store (injected or composed)
- [ ] `clear()` removes all data from the store
- [ ] `write(GraphModel)` is idempotent — calling twice with the same model
      replaces, does not duplicate
- [ ] All `GraphReader` query methods return unmodifiable views
- [ ] Thread-safe (use `ConcurrentHashMap` and `CopyOnWriteArrayList`)
- [ ] Unit tests cover all reader query methods
- [ ] Unit tests cover `clear()` behaviour

## Design

### InMemoryStore (package-private)
Internal shared store. Not part of the public API.

```java
package io.eventus.core.memory;

// holds ConcurrentHashMap<String, ModuleNode> modules
// holds ConcurrentHashMap<String, EventNode>  events
// holds CopyOnWriteArrayList<EventEdge>       edges
// holds CopyOnWriteArrayList<PublicationRecord> publications
```

### InMemoryGraphWriter
```java
package io.eventus.core.memory;

public class InMemoryGraphWriter implements GraphWriter {
    private final InMemoryStore store;

    public InMemoryGraphWriter(InMemoryStore store) { ... }

    @Override
    public void write(GraphModel model) {
        // replace entire store contents with model contents
    }

    @Override
    public void clear() { store.clear(); }
}
```

### InMemoryGraphReader
```java
package io.eventus.core.memory;

public class InMemoryGraphReader implements GraphReader {
    private final InMemoryStore store;

    public InMemoryGraphReader(InMemoryStore store) { ... }

    // implement all GraphReader methods
    // getEdgesForEvent: filter edges where eventId matches
    // getIncompletePublications: filter where status != COMPLETED
}
```

## Convenience Factory

Add a static factory in `eventus-core` for easy wiring:

```java
package io.eventus.core.memory;

public final class InMemoryGraph {
    public static InMemoryGraphWriter writer() { ... }
    public static InMemoryGraphReader reader(InMemoryGraphWriter writer) { ... }
    // shared store is created internally
}
```

## Tests Required

### InMemoryGraphWriterTest
- write a GraphModel with 2 modules, 1 event, 2 edges → reader returns all
- write again with different data → old data is replaced, not appended
- clear() → all reader methods return empty lists

### InMemoryGraphReaderTest
- getEdgesForEvent returns only edges matching the eventId
- getIncompletePublications returns only INCOMPLETE and STALE records
- all returned lists throw UnsupportedOperationException on mutation attempt

## Done When
`mvn test -pl eventus-core` passes with all new tests green.
