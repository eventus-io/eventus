package io.eventus.core.model;

public record ModuleNode(
        String id,
        String name,
        int beanCount,
        int aggregateCount,
        Status status
) {
    public enum Status { HEALTHY, WARNING, ERROR }
}
