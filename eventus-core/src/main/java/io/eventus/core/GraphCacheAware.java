package io.eventus.core;

/**
 * Implemented by analyzers that maintain a derived cache over the graph.
 * {@link GraphWriter} implementations call {@link #invalidateCache()} after
 * every write so stale results are never served across a graph refresh.
 */
public interface GraphCacheAware {
    void invalidateCache();
}
