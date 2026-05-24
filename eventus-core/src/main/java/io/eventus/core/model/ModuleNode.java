package io.eventus.core.model;

public record ModuleNode(
        String id,
        String name,
        int beanCount,
        int aggregateCount,
        ModuleStatus status
) {}
