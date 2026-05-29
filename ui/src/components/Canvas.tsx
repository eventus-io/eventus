import { useMemo, useCallback, useRef, useState, useEffect, useLayoutEffect } from 'react';
import type { Module, RichEvent, CanvasEdge, Layout, Selection } from '../types';
import { displayId } from '../hooks/useAggregatedTopology';
import { STATUS_COLOR } from './Pills';

const NODE_W = 140;
const NODE_H = 28;
const LAYOUT_W = 1000;
const LAYOUT_H = 600;

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
  // per-node service colors (hex); absent = single-backend (no indicator needed)
  moduleColors?: Map<string, string>;
  eventColors?: Map<string, string | null>;
  // for legend
  backendColors?: Map<string, string>;
  backendNames?: Map<string, string>;
}

export function Canvas({
  modules, richEvents, edges,
  layout, visibleModuleIds, visibleEventIds,
  selected, hovered,
  setSelected, setHovered,
  totalModules, totalEvents, incompleteCount,
  moduleColors, eventColors,
  backendColors, backendNames,
}: CanvasProps) {
  const multiBackend = backendColors != null && backendColors.size > 1;
  const focus = selected ?? hovered;
  const svgRef = useRef<SVGSVGElement>(null);

  // ── transform state ──────────────────────────────────────────────────────
  const [tf, setTf] = useState({ x: 0, y: 0, scale: 1 });

  // Compute initial fit-to-viewport once the SVG has a real size
  useLayoutEffect(() => {
    const svg = svgRef.current;
    if (!svg) return;
    const { width, height } = svg.getBoundingClientRect();
    if (!width || !height) return;
    const scale = Math.min(width / LAYOUT_W, height / LAYOUT_H, 1);
    setTf({
      x: (width  - LAYOUT_W * scale) / 2,
      y: (height - LAYOUT_H * scale) / 2,
      scale,
    });
  }, []);

  // ── wheel zoom (centered on cursor) ──────────────────────────────────────
  const onWheel = useCallback((e: WheelEvent) => {
    e.preventDefault();
    const svg = svgRef.current;
    if (!svg) return;
    const rect = svg.getBoundingClientRect();
    const mx = e.clientX - rect.left;
    const my = e.clientY - rect.top;
    const factor = e.deltaY < 0 ? 1.12 : 1 / 1.12;
    setTf(prev => {
      const newScale = Math.min(6, Math.max(0.15, prev.scale * factor));
      const px = (mx - prev.x) / prev.scale;
      const py = (my - prev.y) / prev.scale;
      return { scale: newScale, x: mx - px * newScale, y: my - py * newScale };
    });
  }, []);

  useEffect(() => {
    const svg = svgRef.current;
    if (!svg) return;
    svg.addEventListener('wheel', onWheel, { passive: false });
    return () => svg.removeEventListener('wheel', onWheel);
  }, [onWheel]);

  // ── drag-to-pan ───────────────────────────────────────────────────────────
  const drag = useRef({ active: false, x: 0, y: 0, moved: false });
  const [isDragging, setIsDragging] = useState(false);

  const onMouseDown = useCallback((e: React.MouseEvent<SVGSVGElement>) => {
    if (e.button !== 0) return;
    drag.current = { active: true, x: e.clientX, y: e.clientY, moved: false };
    setIsDragging(true);
  }, []);

  const onMouseMove = useCallback((e: React.MouseEvent<SVGSVGElement>) => {
    const d = drag.current;
    if (!d.active) return;
    const dx = e.clientX - d.x;
    const dy = e.clientY - d.y;
    if (!d.moved && Math.hypot(dx, dy) < 4) return;
    d.moved = true;
    d.x = e.clientX;
    d.y = e.clientY;
    setTf(prev => ({ ...prev, x: prev.x + dx, y: prev.y + dy }));
  }, []);

  const onMouseUp = useCallback(() => {
    drag.current.active = false;
    setIsDragging(false);
  }, []);

  // ── zoom controls ─────────────────────────────────────────────────────────
  const zoomBy = (factor: number) => {
    const svg = svgRef.current;
    if (!svg) return;
    const { width, height } = svg.getBoundingClientRect();
    setTf(prev => {
      const newScale = Math.min(6, Math.max(0.15, prev.scale * factor));
      const px = (width / 2 - prev.x) / prev.scale;
      const py = (height / 2 - prev.y) / prev.scale;
      return { scale: newScale, x: width / 2 - px * newScale, y: height / 2 - py * newScale };
    });
  };

  const fitView = () => {
    const svg = svgRef.current;
    if (!svg) return;
    const { width, height } = svg.getBoundingClientRect();
    const scale = Math.min(width / LAYOUT_W, height / LAYOUT_H, 1);
    setTf({ x: (width - LAYOUT_W * scale) / 2, y: (height - LAYOUT_H * scale) / 2, scale });
  };

  // ── focus / dim helpers ───────────────────────────────────────────────────
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

  const scaleLabel = `${Math.round(tf.scale * 100)}%`;

  return (
    <div style={{ position: 'relative', flex: 1, overflow: 'hidden', background: 'var(--eventus-bg)' }}>
      {/* dot grid — static, stays fixed under the zoomed content */}
      <div style={{
        position: 'absolute', inset: 0,
        backgroundImage: 'radial-gradient(circle, var(--eventus-border) 1px, transparent 1px)',
        backgroundSize: '24px 24px',
        opacity: 0.55,
        pointerEvents: 'none',
      }} />

      <svg
        ref={svgRef}
        width="100%"
        height="100%"
        style={{
          position: 'relative', display: 'block',
          cursor: isDragging ? 'grabbing' : 'grab',
          userSelect: 'none',
        }}
        onMouseDown={onMouseDown}
        onMouseMove={onMouseMove}
        onMouseUp={onMouseUp}
        onMouseLeave={onMouseUp}
        onClick={() => { if (!drag.current.moved) setSelected(null); }}
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

        {/* All content inside the pannable/zoomable group */}
        <g transform={`translate(${tf.x},${tf.y}) scale(${tf.scale})`}>
          {/* Edges — rendered first so module nodes paint over them */}
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
            const dim         = isDim('module', m.id);
            const isSel       = selected?.type === 'module' && selected.id === m.id;
            const statusColor = STATUS_COLOR[m.status] ?? 'var(--eventus-fg-dim)';
            return (
              <g
                key={m.id}
                onMouseEnter={() => setHovered({ type: 'module', id: m.id })}
                onMouseLeave={() => setHovered(null)}
                onClick={e => { e.stopPropagation(); if (!drag.current.moved) setSelected({ type: 'module', id: m.id }); }}
                style={{ cursor: 'pointer', opacity: dim ? 0.25 : 1, transition: 'opacity .15s' }}
              >
                {isSel && (
                  <rect
                    x={p.x - NODE_W / 2 - 3} y={p.y - NODE_H / 2 - 3}
                    width={NODE_W + 6} height={NODE_H + 6}
                    rx={7} fill="none"
                    stroke="var(--eventus-accent)" strokeWidth={1} strokeDasharray="3 3" opacity={0.6}
                  />
                )}
                <rect
                  x={p.x - NODE_W / 2} y={p.y - NODE_H / 2}
                  width={NODE_W} height={NODE_H}
                  rx={5}
                  fill="var(--eventus-node-module-fill)"
                  stroke={statusColor}
                  strokeWidth={isSel ? 2 : 1.5}
                />
                {/* Service color bar (multi-backend only) */}
                {multiBackend && moduleColors?.has(m.id) && (
                  <rect
                    x={p.x - NODE_W / 2 + 1} y={p.y - NODE_H / 2 + 1}
                    width={4} height={NODE_H - 2}
                    rx={4}
                    fill={moduleColors.get(m.id)!}
                    opacity={0.85}
                  />
                )}
                <circle cx={p.x - NODE_W / 2 + 11} cy={p.y} r={3.5} fill={statusColor} />
                <text
                  x={p.x + 4} y={p.y}
                  fontFamily="var(--eventus-font-mono)" fontSize="11"
                  fill="var(--eventus-fg)" textAnchor="middle" dominantBaseline="middle"
                >{displayId(m.id)}</text>
              </g>
            );
          })}

          {/* Event nodes */}
          {richEvents.map(ev => {
            const p = layout.events[ev.id];
            if (!p) return null;
            const dim = isDim('event', ev.id);
            const isSel = selected?.type === 'event' && selected.id === ev.id;
            const isCrossService = eventColors?.get(ev.id) === null;
            const svcColor = eventColors?.get(ev.id) ?? null;
            const fill = isCrossService
              ? 'var(--eventus-fg-dim)'
              : ev.incomplete ? 'var(--eventus-danger)' : (svcColor && multiBackend ? svcColor : 'var(--eventus-accent)');
            return (
              <g
                key={ev.id}
                onMouseEnter={() => setHovered({ type: 'event', id: ev.id })}
                onMouseLeave={() => setHovered(null)}
                onClick={e => { e.stopPropagation(); if (!drag.current.moved) setSelected({ type: 'event', id: ev.id }); }}
                style={{ cursor: 'pointer', opacity: dim ? 0.25 : 1, transition: 'opacity .15s' }}
              >
                {isSel && (
                  <circle cx={p.x} cy={p.y} r={11}
                    fill="none" stroke="var(--eventus-accent)" strokeWidth={1} strokeDasharray="3 3" opacity={0.6} />
                )}
                {/* Cross-service events get a dashed outer ring */}
                {isCrossService && !isSel && (
                  <circle cx={p.x} cy={p.y} r={10}
                    fill="none"
                    stroke="var(--eventus-fg-dim)"
                    strokeWidth={1}
                    strokeDasharray="2 3"
                    opacity={0.5}
                  />
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
        </g>
      </svg>

      {/* Zoom controls */}
      <div style={{
        position: 'absolute', top: 12, right: 12,
        display: 'flex', flexDirection: 'column', gap: 4,
        zIndex: 10,
      }}>
        {[
          { label: '+', title: 'Zoom in',  action: () => zoomBy(1.25) },
          { label: '−', title: 'Zoom out', action: () => zoomBy(1 / 1.25) },
          { label: '⊡', title: 'Fit view', action: fitView },
        ].map(btn => (
          <button
            key={btn.label}
            title={btn.title}
            onClick={btn.action}
            style={{
              width: 28, height: 28,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              background: 'var(--eventus-bg-subtle)',
              border: '1px solid var(--eventus-border)',
              borderRadius: 6,
              color: 'var(--eventus-fg)',
              fontFamily: 'var(--eventus-font-mono)',
              fontSize: 14,
              cursor: 'pointer',
              lineHeight: 1,
            }}
          >{btn.label}</button>
        ))}
        <div style={{
          textAlign: 'center',
          fontFamily: 'var(--eventus-font-mono)', fontSize: 10,
          color: 'var(--eventus-fg-dim)',
          marginTop: 2,
        }}>{scaleLabel}</div>
      </div>

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
          <span style={{ width: 10, height: 10, borderRadius: 2, border: `1.5px solid ${STATUS_COLOR.HEALTHY}` }} />healthy
        </span>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 10, height: 10, borderRadius: 2, border: `1.5px solid ${STATUS_COLOR.WARNING}` }} />warning
        </span>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 10, height: 10, borderRadius: 2, border: `1.5px solid ${STATUS_COLOR.ERROR}` }} />error
        </span>
        {/* Service swatches (multi-backend only) */}
        {multiBackend && backendColors && backendNames && (
          <>
            <span style={{ width: 1, height: 12, background: 'var(--eventus-border)' }} />
            {[...backendColors.entries()].map(([id, color]) => (
              <span key={id} style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}>
                <span style={{ width: 8, height: 8, borderRadius: 2, background: color, flexShrink: 0 }} />
                {backendNames.get(id) ?? id}
              </span>
            ))}
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}>
              <svg width="12" height="12" viewBox="0 0 12 12">
                <circle cx="6" cy="6" r="5" fill="none" stroke="var(--eventus-fg-dim)" strokeWidth="1" strokeDasharray="2 2" />
              </svg>
              cross-service
            </span>
          </>
        )}
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
