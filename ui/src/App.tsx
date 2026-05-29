import { useState, useMemo, useEffect } from 'react';
import type { Selection, ActiveView } from './types';
import { useAggregatedTopology } from './hooks/useAggregatedTopology';
import { useRichEvents, useCanvasEdges } from './hooks/useEdges';
import { computeLayout } from './layout';
import { fetchViolations, fetchDrift } from './client';
import { BackendsProvider, useBackends } from './context/BackendsContext';
import { TopBar } from './components/TopBar';
import { NavTabs } from './components/NavTabs';
import { Sidebar } from './components/Sidebar';
import { Canvas } from './components/Canvas';
import { Inspector } from './components/Inspector';
import { ImpactView } from './components/views/ImpactView';
import { ViolationsView } from './components/views/ViolationsView';
import { DriftView } from './components/views/DriftView';
import { PublicationsView } from './components/views/PublicationsView';

function lc(s: string) { return s.toLowerCase(); }

function AppInner() {
  const [activeView, setActiveView] = useState<ActiveView>('graph');
  const [filter, setFilter] = useState('');
  const [selected, setSelected] = useState<Selection | null>(null);
  const [hovered, setHovered] = useState<Selection | null>(null);

  const { backends } = useBackends();
  const { data, errors, refreshKey, refresh } = useAggregatedTopology();

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

  // Violation and drift badge counts — aggregate from all enabled backends
  const [violationCount, setViolationCount] = useState(0);
  const [driftBreachCount, setDriftBreachCount] = useState(0);

  useEffect(() => {
    const enabled = backends.filter(b => b.enabled);
    Promise.all(enabled.map(b => fetchViolations(b.url).catch(() => [])))
      .then(results => setViolationCount(results.flat().length))
      .catch(() => {});
    Promise.all(enabled.map(b => fetchDrift(b.url).catch(() => ({ breachingCount: 0 }))))
      .then(results => setDriftBreachCount(results.reduce((s, r) => s + r.breachingCount, 0)))
      .catch(() => {});
  }, [backends]);

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column' }}>
      <TopBar refreshKey={refreshKey} onRefresh={refresh} errors={errors} />
      <NavTabs
        active={activeView}
        onChange={v => { setActiveView(v); setSelected(null); }}
        incompleteCount={incompleteCount}
        violationCount={violationCount}
        driftBreachCount={driftBreachCount}
      />
      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        {activeView === 'graph' && (
          <>
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
              moduleColors={data?.moduleColors}
              eventColors={data?.eventColors}
              backendColors={data?.backendColors}
              backendNames={data?.backendNames}
            />
            <Inspector
              selected={selected}
              setSelected={setSelected}
              modules={data?.modules ?? []}
              richEvents={richEvents}
              violations={[]}
            />
          </>
        )}
        {activeView === 'impact' && (
          <ImpactView
            modules={data?.modules ?? []}
            richEvents={richEvents}
            backendUrls={data?.backendUrls}
          />
        )}
        {activeView === 'violations' && <ViolationsView />}
        {activeView === 'drift' && <DriftView />}
        {activeView === 'publications' && (
          <PublicationsView publications={data?.publications ?? []} />
        )}
      </div>
    </div>
  );
}

export function App() {
  return (
    <BackendsProvider>
      <AppInner />
    </BackendsProvider>
  );
}
