package io.eventus.core.memory;

public final class InMemoryGraph {

    private InMemoryGraph() {}

    public static InMemoryGraphWriter writer() {
        return new InMemoryGraphWriter(new InMemoryStore());
    }

    public static InMemoryGraphReader reader(InMemoryGraphWriter writer) {
        return new InMemoryGraphReader(writer.store);
    }
}
