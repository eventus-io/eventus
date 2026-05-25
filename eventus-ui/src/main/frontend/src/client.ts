import type { GraphData } from './types';

export async function fetchGraph(signal?: AbortSignal): Promise<GraphData> {
  const res = await fetch('/eventus/api/graph', { signal });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json() as Promise<GraphData>;
}
