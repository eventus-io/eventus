import { useState, useMemo, useEffect } from 'react';
import type { Selection } from './types';
import { useTopology } from './hooks/useTopology';
import { useRichEvents, useCanvasEdges } from './hooks/useEdges';
import { computeLayout } from './layout';
import { TopBar } from './components/TopBar';
import { Sidebar } from './components/Sidebar';
import { Canvas } from './components/Canvas';
import { Inspector } from './components/Inspector';

function lc(s: string) { return s.toLowerCase(); }

export function App() {
  const [filter, setFilter] = useState('');
  const [selected, setSelected] = useState<Selection | null>(null);
  const [hovered, setHovered] = useState<Selection | null>(null);

  const { data, refreshKey, refresh } = useTopology();
  const richEvents = useRichEvents(data);
  const canvasEdges = useCanvasEdges(richEvents);
  const layout = useMemo(
    () => computeLayout(data?.modules ?? [], richEvents),
    [data?.modules, richEvents],
  );

  const visibleModules = useMemo(() => {
    if (!data) return [];
    if (!filter.trim()) return data.modules;
    const q = lc(filter);
    return data.modules.filter(m =>
      lc(m.id).includes(q) || lc(m.name).includes(q) || lc(m.status).includes(q),
    );
  }, [data, filter]);

  const visibleEvents = useMemo(() => {
    if (!filter.trim()) return richEvents;
    const q = lc(filter);
    return richEvents.filter(e =>
      lc(e.simpleName).includes(q) || lc(e.id).includes(q) || lc(e.publisher).includes(q),
    );
  }, [richEvents, filter]);

  const visibleModuleIds = useMemo(() => new Set(visibleModules.map(m => m.id)), [visibleModules]);
  const visibleEventIds  = useMemo(() => new Set(visibleEvents.map(e => e.id)),  [visibleEvents]);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (selected) setSelected(null);
        else if (filter) setFilter('');
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [selected, filter]);

  const incompleteCount = data?.publications.filter(p => p.status !== 'COMPLETED').length ?? 0;

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column' }}>
      <TopBar refreshKey={refreshKey} onRefresh={refresh} />
      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        <Sidebar
          filter={filter}
          setFilter={setFilter}
          selected={selected}
          setSelected={setSelected}
          modules={visibleModules}
          richEvents={visibleEvents}
          publications={data?.publications ?? []}
        />
        <Canvas
          modules={data?.modules ?? []}
          richEvents={richEvents}
          edges={canvasEdges}
          layout={layout}
          visibleModuleIds={visibleModuleIds}
          visibleEventIds={visibleEventIds}
          selected={selected}
          hovered={hovered}
          setSelected={setSelected}
          setHovered={setHovered}
          totalModules={data?.modules.length ?? 0}
          totalEvents={richEvents.length}
          incompleteCount={incompleteCount}
        />
        <Inspector
          selected={selected}
          setSelected={setSelected}
          modules={data?.modules ?? []}
          richEvents={richEvents}
        />
      </div>
    </div>
  );
}
