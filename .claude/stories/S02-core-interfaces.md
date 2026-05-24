# S02 — Core Interfaces & Value Objects

## Goal
Define the framework-agnostic domain model and interfaces in `eventus-core`.
Nothing in this module may import Spring or any other framework.

## Acceptance Criteria
- [ ] All value objects exist as Java 21 records
- [ ] `EventGraphExtractor` interface defined
- [ ] `GraphWriter` interface defined
- [ ] `GraphReader` interface defined
- [ ] `GraphModel` class defined (mutable, used during extraction)
- [ ] `ModuleStatus` enum defined
- [ ] `EdgeType` enum defined
- [ ] Package structure is `io.eventus.core.model` for records/enums, `io.eventus.core` for interfaces
- [ ] No framework imports anywhere in `eventus-core`
- [ ] Unit tests compile and pass

## Value Objects

### ModuleStatus enum
```java
package io.eventus.core.model;

public enum ModuleStatus { HEALTHY, WARNING, ERROR }
```

### EdgeType enum
```java
package io.eventus.core.model;

public enum EdgeType { PUBLISHES, LISTENS_TO }
```

### ModuleNode record
```java
package io.eventus.core.model;

public record ModuleNode(
    String id,
    String name,
    int beanCount,
    int aggregateCount,
    ModuleStatus status
) {}
```

### EventNode record
```java
package io.eventus.core.model;

public record EventNode(
    String id,       // fully qualified class name of the event
    String name,     // simple class name
    String publisherModuleId
) {}
```

### EventEdge record
```java
package io.eventus.core.model;

public record EventEdge(
    String id,
    String eventId,
    String fromModuleId,
    String toModuleId,
    EdgeType edgeType
) {}
```

### PublicationRecord record
```java
package io.eventus.core.model;

import java.time.Instant;

public record PublicationRecord(
    String id,
    String eventType,
    String listenerName,
    String moduleId,
    PublicationStatus status,
    Instant publishedAt
) {}
```

### PublicationStatus enum
```java
package io.eventus.core.model;

public enum PublicationStatus { COMPLETED, INCOMPLETE, STALE }
```

## GraphModel class

```java
package io.eventus.core.model;

import java.util.*;

public class GraphModel {
    private final List<ModuleNode> modules = new ArrayList<>();
    private final List<EventNode> events = new ArrayList<>();
    private final List<EventEdge> edges = new ArrayList<>();
    private final List<PublicationRecord> publications = new ArrayList<>();

    public void addModule(ModuleNode node) { modules.add(node); }
    public void addEvent(EventNode node) { events.add(node); }
    public void addEdge(EventEdge edge) { edges.add(edge); }
    public void addPublication(PublicationRecord record) { publications.add(record); }

    // unmodifiable views
    public List<ModuleNode> modules() { return Collections.unmodifiableList(modules); }
    public List<EventNode> events() { return Collections.unmodifiableList(events); }
    public List<EventEdge> edges() { return Collections.unmodifiableList(edges); }
    public List<PublicationRecord> publications() { return Collections.unmodifiableList(publications); }
}
```

## Interfaces

### EventGraphExtractor
```java
package io.eventus.core;

import io.eventus.core.model.GraphModel;

public interface EventGraphExtractor {
    GraphModel extract();
    default String name() { return getClass().getSimpleName(); }
}
```

### GraphWriter
```java
package io.eventus.core;

import io.eventus.core.model.*;

public interface GraphWriter {
    void write(GraphModel model);
    void clear();
}
```

### GraphReader
```java
package io.eventus.core;

import io.eventus.core.model.*;
import java.util.List;

public interface GraphReader {
    List<ModuleNode> getModules();
    List<EventNode> getEvents();
    List<EventEdge> getEdges();
    List<EventEdge> getEdgesForEvent(String eventId);
    List<PublicationRecord> getPublications();
    List<PublicationRecord> getIncompletePublications();
}
```

## Tests Required

`GraphModelTest` — verify that adding nodes/edges populates the model correctly
and that the returned lists are unmodifiable.

## Done When
`mvn test -pl eventus-core` passes with all assertions green.
