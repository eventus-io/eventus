import { useMemo, useCallback } from 'react';
import type { Module, RichEvent, CanvasEdge, Layout, Selection } from '../types';
import { STATUS_COLOR } from './Pills';

interface CanvasProps {
  modules: Module[];
  richEvents: RichEvent[];
  edges: CanvasEdge[];
  layout: Layout;
  visibleModuleIds: Set<string>;
  visibleEventIds: Set<string>;
  selected: Selection | null;
  hovered: Selection | null;
  setSelected: (s: Selection | null) => void;
  setHovered: (s: Selection | null) => void;
  totalModules: number;
  totalEvents: number;
  incompleteCount: number;
}

export function Canvas({
  modules, richEvents, edges, layout,
  visibleModuleIds, visibleEventIds,
  selected, hovered,
  setSelected, setHovered,
  totalModules, totalEvents, incompleteCount,
}: CanvasProps) {
  const focus = selected ?? hovered;

  const related = useMemo(() => {
    if (!focus) return null;
    const mods = new Set<string>();
    const evs = new Set<string>();
    if (focus.type === 'module') {
      mods.add(focus.id);
      for (const ev of richEvents) {
        if (ev.publisher === focus.id) {
          evs.add(ev.id);
          for (const c of ev.consumers) mods.add(c);
        }
        if (ev.consumers.includes(focus.id)) {
          evs.add(ev.id);
          mods.add(ev.publisher);
        }
      }
    } else {
      const ev = richEvents.find(e => e.id === focus.id);
      if (ev) {
        evs.add(ev.id);
        mods.add(ev.publisher);
        for (const c of ev.consumers) mods.add(c);
      }
    }
    return { mods, evs };
  }, [focus, richEvents]);

  const isDim = useCallback((kind: 'module' | 'event', id: string): boolean => {
    if (kind === 'module' && !visibleModuleIds.has(id)) return true;
    if (kind === 'event'  && !visibleEventIds.has(id))  return true;
    if (related) {
      if (kind === 'module') return !related.mods.has(id);
      if (kind === 'event')  return !related.evs.has(id);
    }
    return false;
  }, [related, visibleModuleIds, visibleEventIds]);

  const isEdgeActive = useCallback((edge: CanvasEdge): boolean => {
    if (!focus) return false;
    if (focus.type === 'event' && edge.eventId === focus.id) return true;
    if (focus.type === 'module') {
      if (edge.kind === 'publish'   && edge.from.id === focus.id) return true;
      if (edge.kind === 'subscribe' && edge.to.id   === focus.id) return true;
    }
    return false;
  }, [focus]);

  const posFor = (n: { type: 'module' | 'event'; id: string }) =>
    n.type === 'module' ? layout.modules[n.id] : layout.events[n.id];

  return (
    <div style={{ position: 'relative', flex: 1, overflow: 'hidden', background: 'var(--eventus-bg)' }}>
      {/* dot grid */}
      <div style={{
        position: 'absolute', inset: 0,
        backgroundImage: 'radial-gradient(circle, var(--eventus-border) 1px, transparent 1px)',
        backgroundSize: '24px 24px',
        opacity: 0.55,
      }} />

      <svg
        width="100%"
        height="100%"
        viewBox="0 0 1000 600"
        preserveAspectRatio="xMidYMid meet"
        style={{ position: 'relative', display: 'block' }}
        onClick={() => setSelected(null)}
      >
        <defs>
          <marker id="ar-pub"     markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto">
            <path d="M0 0 L6 3 L0 6 Z" fill="var(--eventus-accent)" />
          </marker>
          <marker id="ar-pub-dim" markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto">
            <path d="M0 0 L6 3 L0 6 Z" fill="var(--eventus-accent-dim)" />
          </marker>
          <marker id="ar-sub"     markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto">
            <path d="M0 0 L6 3 L0 6 Z" fill="var(--eventus-info)" />
          </marker>
          <marker id="ar-sub-dim" markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto">
            <path d="M0 0 L6 3 L0 6 Z" fill="color-mix(in oklch, var(--eventus-info) 35%, transparent)" />
          </marker>
        </defs>

        {/* Edges */}
        {edges.map((edge, i) => {
          const a = posFor(edge.from);
          const b = posFor(edge.to);
          if (!a || !b) return null;
          const active = isEdgeActive(edge);
          const anyDim = isDim(edge.from.type, edge.from.id) || isDim(edge.to.type, edge.to.id);
          const isPub = edge.kind === 'publish';
          const color = isPub
            ? (anyDim ? 'var(--eventus-accent-dim)' : 'var(--eventus-accent)')
            : (anyDim ? 'color-mix(in oklch, var(--eventus-info) 35%, transparent)' : 'var(--eventus-info)');
          const marker = isPub
            ? (anyDim ? 'ar-pub-dim' : 'ar-pub')
            : (anyDim ? 'ar-sub-dim' : 'ar-sub');
          return (
            <line
              key={i}
              x1={a.x} y1={a.y} x2={b.x} y2={b.y}
              stroke={color}
              strokeWidth={active ? 2 : 1.5}
              strokeDasharray={isPub ? undefined : '4 4'}
              markerEnd={`url(#${marker})`}
              style={{ transition: 'stroke .15s, stroke-width .15s' }}
            />
          );
        })}

        {/* Module nodes */}
        {modules.map(m => {
          const p = layout.modules[m.id];
          if (!p) return null;
          const dim = isDim('module', m.id);
          const isSel = selected?.type === 'module' && selected.id === m.id;
          const stroke = isSel ? 'var(--eventus-accent)' : (STATUS_COLOR[m.status] ?? 'var(--eventus-fg-dim)');
          const w = 78;
          return (
            <g
              key={m.id}
              onMouseEnter={() => setHovered({ type: 'module', id: m.id })}
              onMouseLeave={() => setHovered(null)}
              onClick={e => { e.stopPropagation(); setSelected({ type: 'module', id: m.id }); }}
              style={{ cursor: 'pointer', opacity: dim ? 0.25 : 1, transition: 'opacity .15s' }}
            >
              {isSel && (
                <rect x={p.x - w/2 - 5} y={p.y - 19} width={w + 10} height={38} rx={9}
                  fill="none" stroke="var(--eventus-accent)" strokeWidth={1} strokeDasharray="3 3" opacity={0.6} />
              )}
              <rect
                x={p.x - w/2} y={p.y - 14} width={w} height={28} rx={6}
                fill="var(--eventus-bg-raised)" stroke={stroke} strokeWidth={1.5}
              />
              <text
                x={p.x} y={p.y + 4}
                fontFamily="var(--eventus-font-mono)" fontSize="11"
                fill="var(--eventus-fg)" textAnchor="middle"
              >{m.id}</text>
            </g>
          );
        })}

        {/* Event nodes */}
        {richEvents.map(ev => {
          const p = layout.events[ev.id];
          if (!p) return null;
          const dim = isDim('event', ev.id);
          const isSel = selected?.type === 'event' && selected.id === ev.id;
          const fill = ev.incomplete ? 'var(--eventus-danger)' : 'var(--eventus-accent)';
          return (
            <g
              key={ev.id}
              onMouseEnter={() => setHovered({ type: 'event', id: ev.id })}
              onMouseLeave={() => setHovered(null)}
              onClick={e => { e.stopPropagation(); setSelected({ type: 'event', id: ev.id }); }}
              style={{ cursor: 'pointer', opacity: dim ? 0.25 : 1, transition: 'opacity .15s' }}
            >
              {isSel && (
                <circle cx={p.x} cy={p.y} r={11}
                  fill="none" stroke="var(--eventus-accent)" strokeWidth={1} strokeDasharray="3 3" opacity={0.6} />
              )}
              <circle
                cx={p.x} cy={p.y} r={6.5}
                fill={fill}
                stroke={isSel ? 'var(--eventus-accent)' : 'transparent'}
                strokeWidth={1.5}
              />
              <text
                x={p.x} y={p.y + 22}
                fontFamily="var(--eventus-font-mono)" fontSize="10"
                fill="var(--eventus-fg-mute)" textAnchor="middle"
              >{ev.simpleName}</text>
            </g>
          );
        })}
      </svg>

      {/* Legend */}
      <div style={{
        position: 'absolute', bottom: 50, left: 20,
        background: 'color-mix(in oklch, var(--eventus-bg) 88%, transparent)',
        backdropFilter: 'blur(8px)',
        border: '1px solid var(--eventus-border)', borderRadius: 8,
        padding: '8px 12px',
        display: 'flex', alignItems: 'center', gap: 16,
        fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
        color: 'var(--eventus-fg-mute)',
      }}>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 18, height: 1.5, background: 'var(--eventus-accent)' }} />
          publishes
        </span>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 18, height: 0, borderTop: '1.5px dashed var(--eventus-info)' }} />
          subscribes
        </span>
        <span style={{ width: 1, height: 12, background: 'var(--eventus-border)' }} />
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 10, height: 10, borderRadius: 2, border: '1.5px solid var(--eventus-success)' }} />healthy
        </span>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 10, height: 10, borderRadius: 2, border: '1.5px solid var(--eventus-warn)' }} />warning
        </span>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 10, height: 10, borderRadius: 2, border: '1.5px solid var(--eventus-danger)' }} />error
        </span>
      </div>

      {/* Footer */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0,
        padding: '8px 18px',
        borderTop: '1px solid var(--eventus-border)',
        background: 'var(--eventus-bg-subtle)',
        display: 'flex', justifyContent: 'space-between', gap: 16,
        fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
        color: 'var(--eventus-fg-dim)',
        whiteSpace: 'nowrap',
      }}>
        <span>{totalModules} modules · {totalEvents} events · {incompleteCount} incomplete</span>
        <span>poll 2s</span>
      </div>
    </div>
  );
}
