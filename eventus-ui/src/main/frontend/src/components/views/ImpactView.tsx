import { useState, useEffect } from 'react';
import type { Module, RichEvent, EventImpactResult, ModuleImpactResult } from '../../types';
import { fetchEventImpact, fetchModuleImpact } from '../../client';
import { StatusPill } from '../Pills';

interface ImpactViewProps {
  modules: Module[];
  richEvents: RichEvent[];
}

type ImpactSelection =
  | { kind: 'module'; id: string }
  | { kind: 'event'; id: string }
  | null;

export function ImpactView({ modules, richEvents }: ImpactViewProps) {
  const [sel, setSel] = useState<ImpactSelection>(null);
  const [moduleResult, setModuleResult] = useState<ModuleImpactResult | null>(null);
  const [eventResult, setEventResult]   = useState<EventImpactResult   | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState<string | null>(null);

  useEffect(() => {
    if (!sel) { setModuleResult(null); setEventResult(null); return; }
    setLoading(true);
    setError(null);
    const p = sel.kind === 'module'
      ? fetchModuleImpact(sel.id).then(r => { setModuleResult(r); setEventResult(null); })
      : fetchEventImpact(sel.id).then(r => { setEventResult(r); setModuleResult(null); });
    p.catch(e => setError(String(e))).finally(() => setLoading(false));
  }, [sel]);

  return (
    <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
      {/* Left: selector */}
      <div style={{
        width: 260, flexShrink: 0,
        background: 'var(--eventus-bg-subtle)',
        borderRight: '1px solid var(--eventus-border)',
        overflowY: 'auto', padding: '14px 8px',
      }}>
        <SectionLabel>Modules</SectionLabel>
        {modules.map(m => (
          <PickerRow
            key={m.id}
            label={m.id}
            sub={<StatusPill status={m.status} />}
            active={sel?.kind === 'module' && sel.id === m.id}
            onClick={() => setSel({ kind: 'module', id: m.id })}
          />
        ))}
        <SectionLabel>Events</SectionLabel>
        {richEvents.map(e => (
          <PickerRow
            key={e.id}
            label={e.simpleName}
            sub={<span style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 10, color: 'var(--eventus-fg-dim)' }}>{e.publisher}</span>}
            active={sel?.kind === 'event' && sel.id === e.id}
            onClick={() => setSel({ kind: 'event', id: e.id })}
          />
        ))}
      </div>

      {/* Right: impact detail */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '28px 36px' }}>
        {!sel && (
          <EmptyState message="Select a module or event to analyse its impact on the topology." />
        )}
        {loading && <Spinner />}
        {error && (
          <div style={{ color: 'var(--eventus-danger)', fontFamily: 'var(--eventus-font-mono)', fontSize: 12 }}>{error}</div>
        )}
        {moduleResult && !loading && <ModuleImpact r={moduleResult} />}
        {eventResult  && !loading && <EventImpact  r={eventResult}  />}
      </div>
    </div>
  );
}

function ModuleImpact({ r }: { r: ModuleImpactResult }) {
  return (
    <div style={{ maxWidth: 680 }}>
      <Eyebrow>MODULE IMPACT</Eyebrow>
      <h2 style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 22, color: 'var(--eventus-fg)', margin: '4px 0 6px', letterSpacing: '-0.02em' }}>{r.moduleId}</h2>
      <div style={{ fontFamily: 'var(--eventus-font-sans)', fontSize: 13, color: 'var(--eventus-fg-mute)', marginBottom: 28 }}>
        Changes to this module could affect <strong style={{ color: 'var(--eventus-fg)' }}>{r.totalAffectedModules}</strong> downstream module{r.totalAffectedModules !== 1 ? 's' : ''}.
      </div>

      <SectionTitle>Published Events · {r.publishedEvents.length}</SectionTitle>
      {r.publishedEvents.length === 0 ? <Empty /> : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 28 }}>
          {r.publishedEvents.map(e => (
            <Card key={e.eventId}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 }}>
                <div>
                  <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 13, color: 'var(--eventus-fg)', marginBottom: 2 }}>{e.eventName}</div>
                  <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 10, color: 'var(--eventus-fg-dim)' }}>{e.eventId}</div>
                </div>
                <span style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 11, color: 'var(--eventus-fg-mute)', flexShrink: 0 }}>
                  {e.directListeners} listener{e.directListeners !== 1 ? 's' : ''}
                </span>
              </div>
              {e.listenerModuleIds.length > 0 && (
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 10 }}>
                  {e.listenerModuleIds.map(id => <ModuleTag key={id} id={id} />)}
                </div>
              )}
            </Card>
          ))}
        </div>
      )}

      <SectionTitle>Downstream Modules · {r.downstreamModules.length}</SectionTitle>
      {r.downstreamModules.length === 0 ? <Empty /> : (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {r.downstreamModules.map(d => (
            <div key={d.moduleId} style={{
              padding: '8px 14px', borderRadius: 8,
              border: '1px solid var(--eventus-border)',
              background: 'var(--eventus-bg-raised)',
            }}>
              <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 12, color: 'var(--eventus-fg)' }}>{d.moduleId}</div>
              <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 10, color: 'var(--eventus-fg-dim)', marginTop: 2 }}>{d.relationshipType}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function EventImpact({ r }: { r: EventImpactResult }) {
  return (
    <div style={{ maxWidth: 680 }}>
      <Eyebrow>EVENT IMPACT</Eyebrow>
      <h2 style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 22, color: 'var(--eventus-fg)', margin: '4px 0 6px', letterSpacing: '-0.02em' }}>{r.eventName}</h2>
      <div style={{ fontFamily: 'var(--eventus-font-sans)', fontSize: 13, color: 'var(--eventus-fg-mute)', marginBottom: 28 }}>
        Published by <strong style={{ color: 'var(--eventus-fg)' }}>{r.publisherModuleId}</strong> · <strong style={{ color: 'var(--eventus-fg)' }}>{r.directListeners}</strong> direct listener{r.directListeners !== 1 ? 's' : ''} · <strong style={{ color: 'var(--eventus-fg)' }}>{r.indirectConsumers}</strong> indirect consumers
      </div>

      <SectionTitle>Affected Modules · {r.affectedModules.length}</SectionTitle>
      {r.affectedModules.length === 0 ? (
        <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 12, color: 'var(--eventus-warn)', fontStyle: 'italic' }}>
          No consumers — this event is published but never consumed.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {r.affectedModules.map(m => (
            <Card key={m.moduleId}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 13, color: 'var(--eventus-fg)' }}>{m.moduleId}</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 10, color: 'var(--eventus-fg-dim)' }}>{m.relationshipType}</span>
                  {m.isDirectListener && (
                    <span style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 10, padding: '2px 7px', borderRadius: 999, background: 'color-mix(in oklch, var(--eventus-accent) 14%, transparent)', color: 'var(--eventus-accent)', border: '1px solid color-mix(in oklch, var(--eventus-accent) 28%, transparent)' }}>direct</span>
                  )}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── tiny helpers ──────────────────────────────────────────

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <div style={{
      fontFamily: 'var(--eventus-font-mono)', fontSize: 10, letterSpacing: '0.14em', textTransform: 'uppercase',
      color: 'var(--eventus-fg-dim)', margin: '16px 12px 8px',
    }}>{children}</div>
  );
}

interface PickerRowProps {
  label: string;
  sub: React.ReactNode;
  active: boolean;
  onClick: () => void;
}

function PickerRow({ label, sub, active, onClick }: PickerRowProps) {
  return (
    <button onClick={onClick} style={{
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      padding: '7px 10px', borderRadius: 6, width: '100%',
      background: active ? 'var(--eventus-bg-inset)' : 'transparent',
      color: active ? 'var(--eventus-fg)' : 'var(--eventus-fg-mute)',
      fontFamily: 'var(--eventus-font-mono)', fontSize: 12,
      border: 0, borderLeft: active ? '2px solid var(--eventus-accent)' : '2px solid transparent',
      paddingLeft: active ? 14 : 12, cursor: 'pointer', textAlign: 'left',
    }}
      onMouseEnter={e => { if (!active) (e.currentTarget as HTMLElement).style.background = 'rgba(255,255,255,0.025)'; }}
      onMouseLeave={e => { if (!active) (e.currentTarget as HTMLElement).style.background = 'transparent'; }}
    >
      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{label}</span>
      <span style={{ flexShrink: 0, marginLeft: 8 }}>{sub}</span>
    </button>
  );
}

function Eyebrow({ children }: { children: React.ReactNode }) {
  return <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 10, letterSpacing: '0.14em', textTransform: 'uppercase', color: 'var(--eventus-fg-dim)', marginBottom: 8 }}>{children}</div>;
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 10, letterSpacing: '0.14em', textTransform: 'uppercase', color: 'var(--eventus-fg-dim)', marginBottom: 12, marginTop: 0 }}>{children}</div>;
}

function Card({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ padding: '12px 16px', borderRadius: 8, border: '1px solid var(--eventus-border)', background: 'var(--eventus-bg-raised)' }}>
      {children}
    </div>
  );
}

function ModuleTag({ id }: { id: string }) {
  return (
    <span style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 11, padding: '3px 8px', borderRadius: 6, border: '1px solid var(--eventus-border)', background: 'var(--eventus-bg-inset)', color: 'var(--eventus-fg-mute)' }}>{id}</span>
  );
}

function EmptyState({ message }: { message: string }) {
  return (
    <div style={{ paddingTop: 60, textAlign: 'center', maxWidth: 360, margin: '0 auto' }}>
      <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 11, letterSpacing: '0.14em', textTransform: 'uppercase', color: 'var(--eventus-fg-dim)', marginBottom: 10 }}>IMPACT ANALYSIS</div>
      <div style={{ fontFamily: 'var(--eventus-font-sans)', fontSize: 14, color: 'var(--eventus-fg-mute)', lineHeight: 1.6 }}>{message}</div>
    </div>
  );
}

function Empty() {
  return <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 11, color: 'var(--eventus-fg-dim)', fontStyle: 'italic', marginBottom: 24 }}>none</div>;
}

function Spinner() {
  return <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 12, color: 'var(--eventus-fg-dim)', padding: '8px 0' }}>loading…</div>;
}
