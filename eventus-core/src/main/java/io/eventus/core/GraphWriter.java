package io.eventus.core;

import io.eventus.core.model.GraphModel;

public interface GraphWriter {
    void write(GraphModel model);
    void clear();
}
