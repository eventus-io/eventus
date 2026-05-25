import { useState, useEffect, useCallback } from 'react';
import { fetchGraph } from '../client';
import type { GraphData } from '../types';

interface TopologyState {
  data: GraphData | null;
  error: string | null;
  refreshKey: number;
  refresh: () => void;
}

const POLL_MS = 2000;

export function useTopology(): TopologyState {
  const [data, setData] = useState<GraphData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const refresh = useCallback(() => setRefreshKey(k => k + 1), []);

  useEffect(() => {
    const controller = new AbortController();
    let alive = true;

    async function poll() {
      try {
        const result = await fetchGraph(controller.signal);
        if (alive) {
          setData(result);
          setError(null);
        }
      } catch (err) {
        if (alive && !controller.signal.aborted) {
          setError(err instanceof Error ? err.message : 'fetch failed');
        }
      }
    }

    void poll();
    const timer = setInterval(() => void poll(), POLL_MS);

    return () => {
      alive = false;
      controller.abort();
      clearInterval(timer);
    };
  }, [refreshKey]);

  return { data, error, refreshKey, refresh };
}
