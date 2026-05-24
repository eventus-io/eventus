package io.eventus.generic;

import io.eventus.core.EventGraphExtractor;
import io.eventus.core.model.GraphModel;

/**
 * No-op extractor stub. Annotation-based extraction is planned for v0.3.
 */
public class EmptyExtractor implements EventGraphExtractor {

    @Override
    public void extract(GraphModel model) {
        // intentional no-op
    }
}
