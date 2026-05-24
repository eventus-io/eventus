package io.eventus.core.drift;

import io.eventus.core.model.GraphModel;

public interface BaselineManager {
    BaselineSnapshot loadBaseline();
    void saveBaseline(GraphModel current);
    boolean hasBaseline();
}
