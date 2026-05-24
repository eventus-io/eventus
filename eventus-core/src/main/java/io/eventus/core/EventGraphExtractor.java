package io.eventus.core;

import io.eventus.core.model.GraphModel;

public interface EventGraphExtractor {
    GraphModel extract();

    default String name() {
        return getClass().getSimpleName();
    }
}
