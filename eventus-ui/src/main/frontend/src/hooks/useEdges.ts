import { useMemo } from 'react';
import type { GraphData, RichEvent, CanvasEdge } from '../types';

export function useRichEvents(data: GraphData | null): RichEvent[] {
  return useMemo(() => {
    if (!data) return [];

    const consumersByEvent = new Map<string, string[]>();
    for (const edge of data.edges) {
      if (edge.edgeType === 'LISTENS_TO' && edge.toModuleId) {
        const list = consumersByEvent.get(edge.eventId) ?? [];
        list.push(edge.toModuleId);
        consumersByEvent.set(edge.eventId, list);
      }
    }

    const incompleteEvents = new Set(
      data.publications
        .filter(p => p.status !== 'COMPLETED')
        .map(p => p.eventType),
    );

    return data.events.map(e => ({
      id: e.id,
      simpleName: e.name,
      publisher: e.publisherModuleId,
      consumers: consumersByEvent.get(e.id) ?? [],
      incomplete: incompleteEvents.has(e.id),
    }));
  }, [data]);
}

export function useCanvasEdges(richEvents: RichEvent[]): CanvasEdge[] {
  return useMemo(() => {
    const edges: CanvasEdge[] = [];
    for (const ev of richEvents) {
      edges.push({
        kind: 'publish',
        from: { type: 'module', id: ev.publisher },
        to: { type: 'event', id: ev.id },
        eventId: ev.id,
      });
      for (const c of ev.consumers) {
        edges.push({
          kind: 'subscribe',
          from: { type: 'event', id: ev.id },
          to: { type: 'module', id: c },
          eventId: ev.id,
        });
      }
    }
    return edges;
  }, [richEvents]);
}
