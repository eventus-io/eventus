import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import type { GraphData, Module, EventDef, EdgeDef, Publication } from '../types';
import { fetchGraph } from '../client';
import { useBackends } from '../context/BackendsContext';
import type { Backend } from '../store/backends';

export interface AggregatedData {
  modules: Module[];
  events: EventDef[];
  edges: EdgeDef[];
  publications: Publication[];
  moduleColors: Map<string, string>;      // namespaced moduleId → hex color
  eventColors: Map<string, string | null>; // eventId → hex (null = cross-service)
  backendColors: Map<string, string>;     // backendId → hex
  backendNames: Map<string, string>;      // backendId → display name
  backendUrls: Map<string, string>;       // backendId → base URL
}

// Splits a namespaced ID like "backendId::originalId" → { backendId, originalId }
export function splitId(id: string): { backendId: string; originalId: string } | null {
  const idx = id.indexOf('::');
  if (idx === -1) return null;
  return { backendId: id.slice(0, idx), originalId: id.slice(idx + 2) };
}

// Returns the original (un-namespaced) part of an ID for display purposes
export function displayId(id: string): string {
  return splitId(id)?.originalId ?? id;
}

type TaggedEvent = EventDef & { backendId: string };
type TaggedEdge = EdgeDef & { backendId: string };

const XSVC = 'xsvc'; // prefix for cross-service merged event nodes
const POLL_MS = 2000;

function mergeGraphs(results: Array<{ backend: Backend; data: GraphData }>): AggregatedData {
  const modules: Module[] = [];
  const rawEvents: TaggedEvent[] = [];
  const rawEdges: TaggedEdge[] = [];
  const publications: Publication[] = [];

  const moduleColors = new Map<string, string>();
  const backendColors = new Map<string, string>();
  const backendNames = new Map<string, string>();
  const backendUrls = new Map<string, string>();

  for (const { backend, data } of results) {
    backendColors.set(backend.id, backend.color);
    backendNames.set(backend.id, backend.name);
    backendUrls.set(backend.id, backend.url);

    for (const m of data.modules) {
      const nsId = `${backend.id}::${m.id}`;
      modules.push({ ...m, id: nsId });
      moduleColors.set(nsId, backend.color);
    }

    for (const e of data.events) {
      rawEvents.push({
        ...e,
        id: `${backend.id}::${e.id}`,
        publisherModuleId: `${backend.id}::${e.publisherModuleId}`,
        backendId: backend.id,
      });
    }

    for (const edge of data.edges) {
      rawEdges.push({
        ...edge,
        id: `${backend.id}::${edge.id}`,
        eventId: `${backend.id}::${edge.eventId}`,
        fromModuleId: edge.fromModuleId ? `${backend.id}::${edge.fromModuleId}` : null,
        toModuleId: edge.toModuleId ? `${backend.id}::${edge.toModuleId}` : null,
        backendId: backend.id,
      });
    }

    for (const p of data.publications) {
      publications.push({
        ...p,
        id: `${backend.id}::${p.id}`,
        moduleId: `${backend.id}::${p.moduleId}`,
      });
    }
  }

  // Group events by simple name to detect cross-service events
  const byName = new Map<string, TaggedEvent[]>();
  for (const ev of rawEvents) {
    const list = byName.get(ev.name) ?? [];
    list.push(ev);
    byName.set(ev.name, list);
  }

  const eventIdRemap = new Map<string, string>();
  const events: EventDef[] = [];
  const eventColors = new Map<string, string | null>();

  for (const [name, group] of byName) {
    const uniqueBackends = new Set(group.map(e => e.backendId));

    if (uniqueBackends.size === 1) {
      // Single-service event: keep as-is
      const e = group[0];
      events.push({ id: e.id, name: e.name, publisherModuleId: e.publisherModuleId });
      eventColors.set(e.id, backendColors.get(e.backendId) ?? null);
      eventIdRemap.set(e.id, e.id);
    } else {
      // Cross-service event: merge into a shared node
      const sharedId = `${XSVC}::${name}`;
      const canonical = group.find(e => e.publisherModuleId) ?? group[0];
      events.push({ id: sharedId, name, publisherModuleId: canonical.publisherModuleId });
      eventColors.set(sharedId, null);
      for (const e of group) eventIdRemap.set(e.id, sharedId);
    }
  }

  // Remap edges to use merged event IDs, deduplicate
  const edgeSeen = new Set<string>();
  const edges: EdgeDef[] = [];
  for (const edge of rawEdges) {
    const remapped: EdgeDef = {
      id: edge.id,
      eventId: eventIdRemap.get(edge.eventId) ?? edge.eventId,
      fromModuleId: edge.fromModuleId,
      toModuleId: edge.toModuleId,
      edgeType: edge.edgeType,
    };
    const key = `${remapped.fromModuleId}|${remapped.toModuleId}|${remapped.eventId}|${remapped.edgeType}`;
    if (!edgeSeen.has(key)) {
      edgeSeen.add(key);
      edges.push(remapped);
    }
  }

  return { modules, events, edges, publications, moduleColors, eventColors, backendColors, backendNames, backendUrls };
}

export function useAggregatedTopology() {
  const { backends } = useBackends();
  const [data, setData] = useState<AggregatedData | null>(null);
  const [errors, setErrors] = useState<Map<string, string>>(new Map());
  const [refreshKey, setRefreshKey] = useState(0);
  const refresh = useCallback(() => setRefreshKey(k => k + 1), []);

  const enabled = useMemo(() => backends.filter(b => b.enabled), [backends]);
  const backendKey = useMemo(() => enabled.map(b => `${b.id}:${b.url}`).join(','), [enabled]);

  // Ref so the effect always sees the latest list without it being a dep
  const enabledRef = useRef(enabled);
  enabledRef.current = enabled;

  useEffect(() => {
    const currentEnabled = enabledRef.current;
    if (currentEnabled.length === 0) {
      setData(null);
      return;
    }

    const controller = new AbortController();
    let alive = true;

    async function poll() {
      const results = await Promise.allSettled(
        currentEnabled.map(b =>
          fetchGraph(controller.signal, b.url).then(d => ({ backend: b, data: d }))
        )
      );
      if (!alive) return;

      const ok: Array<{ backend: Backend; data: GraphData }> = [];
      const errs = new Map<string, string>();
      results.forEach((r, i) => {
        if (r.status === 'fulfilled') ok.push(r.value);
        else errs.set(currentEnabled[i].id, r.reason instanceof Error ? r.reason.message : 'fetch failed');
      });

      setErrors(errs);
      if (ok.length > 0) setData(mergeGraphs(ok));
    }

    void poll();
    const timer = setInterval(() => void poll(), POLL_MS);
    return () => {
      alive = false;
      controller.abort();
      clearInterval(timer);
    };
  }, [refreshKey, backendKey]); // eslint-disable-line react-hooks/exhaustive-deps

  return { data, errors, refreshKey, refresh };
}
