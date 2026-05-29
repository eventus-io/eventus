import type { Module, RichEvent, Selection, ViolationItem } from '../types';
import { displayId } from '../hooks/useAggregatedTopology';
import { Icon, ICON_PUBLISH, ICON_SUBSCRIBE } from './Icon';
import { StatusPill, NeutralPill } from './Pills';

interface InspectorProps {
  selected: Selection | null;
  setSelected: (s: Selection | null) => void;
  modules: Module[];
  richEvents: RichEvent[];
  violations?: ViolationItem[];
}

export function Inspector({ selected, setSelected, modules, richEvents, violations = [] }: InspectorProps) {
  const wrap: React.CSSProperties = {
    width: 380,
    background: 'var(--eventus-bg-subtle)',
    borderLeft: '1px solid var(--eventus-border)',
    flexShrink: 0,
    display: 'flex',
    flexDirection: 'column',
    overflowY: 'auto',
  };

  if (!selected) {
    return (
      <div style={{ ...wrap, alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: 32 }}>
        <div>
          <svg width="56" height="56" viewBox="0 0 24 24" fill="none" style={{ margin: '0 auto 18px', opacity: 0.35 }}>
            <path d="M7 12 L18 5"  stroke="var(--eventus-accent)" strokeWidth="1.5" strokeLinecap="round" opacity="0.5"/>
            <path d="M7 12 L18 19" stroke="var(--eventus-accent)" strokeWidth="1.5" strokeLinecap="round" opacity="0.5"/>
            <circle cx="18" cy="5"  r="2" fill="var(--eventus-accent)" opacity="0.6"/>
            <circle cx="18" cy="19" r="2" fill="var(--eventus-accent)" opacity="0.6"/>
            <circle cx="7"  cy="12" r="4.5" fill="var(--eventus-accent)" opacity="0.6"/>
            <circle cx="7"  cy="12" r="1.6" fill="var(--eventus-bg-subtle)"/>
          </svg>
          <div style={{
            fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
            letterSpacing: '0.14em', textTransform: 'uppercase',
            color: 'var(--eventus-fg-dim)', marginBottom: 6,
          }}>NO SELECTION</div>
          <div style={{ fontFamily: 'var(--eventus-font-sans)', fontSize: 13, color: 'var(--eventus-fg-mute)', lineHeight: 1.5, maxWidth: 220 }}>
            Click a module or event in the graph to see its publishers, consumers and raw payload.
          </div>
        </div>
      </div>
    );
  }

  if (selected.type === 'module') {
    const m = modules.find(x => x.id === selected.id);
    if (!m) return null;
    const publishes  = richEvents.filter(e => e.publisher === m.id);
    const subscribes = richEvents.filter(e => e.consumers.includes(m.id));

    // Afferent coupling (Ca): distinct modules listening to this module's events
    const ca = new Set(publishes.flatMap(e => e.consumers)).size;
    // Efferent coupling (Ce): distinct publisher modules this module subscribes to
    const ce = new Set(subscribes.map(e => e.publisher).filter(p => p !== m.id)).size;
    const instability = ca + ce === 0 ? 0 : ce / (ca + ce);

    // Hidden-coupling subscriptions for this module
    const hiddenCouplingEventIds = new Set(
      violations
        .filter(v => v.type === 'HIDDEN_COUPLING' && v.affectedModuleIds[0] === m.id)
        .flatMap(v => v.affectedEventIds),
    );

    const moduleViolations = violations.filter(v => v.affectedModuleIds.includes(m.id));

    return (
      <div style={wrap}>
        <InspectorHeader
          eyebrow="MODULE"
          title={displayId(m.id)}
          subtitle={m.name}
          pills={[
            <StatusPill key="s" status={m.status} />,
            <NeutralPill key="b">{`${m.beanCount} beans · ${m.aggregateCount} aggregate${m.aggregateCount === 1 ? '' : 's'}`}</NeutralPill>,
          ]}
          onClose={() => setSelected(null)}
        />
        <ModuleMetrics ca={ca} ce={ce} instability={instability} violations={moduleViolations} />
        <EventSection
          title={`PUBLISHES · ${publishes.length}`}
          icon={ICON_PUBLISH}
          events={publishes}
          hiddenCouplingIds={new Set()}
          onPick={id => setSelected({ type: 'event', id })}
        />
        <EventSection
          title={`SUBSCRIBES · ${subscribes.length}`}
          icon={ICON_SUBSCRIBE}
          events={subscribes}
          hiddenCouplingIds={hiddenCouplingEventIds}
          onPick={id => setSelected({ type: 'event', id })}
        />
        <RawJson
          path={`/actuator/eventus-modules`}
          obj={{
            id: m.id,
            name: m.name,
            beanCount: m.beanCount,
            aggregateCount: m.aggregateCount,
            status: m.status,
          }}
        />
      </div>
    );
  }

  const ev = richEvents.find(x => x.id === selected.id);
  if (!ev) return null;
  return (
    <div style={wrap}>
      <InspectorHeader
        eyebrow="EVENT"
        title={ev.simpleName}
        subtitle={displayId(ev.id)}
        pills={[
          ev.incomplete
            ? <StatusPill key="s" status="INCOMPLETE" label="INCOMPLETE PUBLICATION" />
            : <NeutralPill key="s">domain event</NeutralPill>,
          <NeutralPill key="p">{`${ev.consumers.length} consumer${ev.consumers.length === 1 ? '' : 's'}`}</NeutralPill>,
        ]}
        onClose={() => setSelected(null)}
      />
      <InspectorRow label="PUBLISHED BY">
        <ModuleChip id={ev.publisher} onClick={() => setSelected({ type: 'module', id: ev.publisher })} />
      </InspectorRow>
      <InspectorRow label={`CONSUMED BY · ${ev.consumers.length}`}>
        {ev.consumers.length === 0 ? (
          <span style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 12, color: 'var(--eventus-danger)' }}>
            no consumers
          </span>
        ) : (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {ev.consumers.map(c => (
              <ModuleChip key={c} id={c} onClick={() => setSelected({ type: 'module', id: c })} />
            ))}
          </div>
        )}
      </InspectorRow>
      <RawJson
        path={`/actuator/eventus-events`}
        obj={{
          id: ev.id,
          name: ev.simpleName,
          publisherModuleId: ev.publisher,
          consumerModuleIds: ev.consumers,
          ...(ev.incomplete ? { incomplete: true } : {}),
        }}
      />
    </div>
  );
}

interface InspectorHeaderProps {
  eyebrow: string;
  title: string;
  subtitle: string;
  pills: React.ReactNode[];
  onClose: () => void;
}

function InspectorHeader({ eyebrow, title, subtitle, pills, onClose }: InspectorHeaderProps) {
  return (
    <div style={{ padding: '18px 22px', borderBottom: '1px solid var(--eventus-border)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div style={{
          fontFamily: 'var(--eventus-font-mono)', fontSize: 10,
          letterSpacing: '0.14em', textTransform: 'uppercase',
          color: 'var(--eventus-fg-dim)', marginBottom: 8,
        }}>{eyebrow}</div>
        <button onClick={onClose} style={{
          background: 'transparent', border: 0, color: 'var(--eventus-fg-dim)',
          cursor: 'pointer', fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
          padding: 0, lineHeight: 1,
        }}>esc</button>
      </div>
      <div style={{
        fontFamily: 'var(--eventus-font-mono)', fontSize: 20,
        letterSpacing: '-0.01em', color: 'var(--eventus-fg)', marginBottom: 6,
      }}>{title}</div>
      <div style={{ fontFamily: 'var(--eventus-font-sans)', fontSize: 13, color: 'var(--eventus-fg-mute)' }}>{subtitle}</div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 12 }}>{pills}</div>
    </div>
  );
}

function InspectorRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div style={{ padding: '16px 22px', borderBottom: '1px solid var(--eventus-border)' }}>
      <div style={{
        fontFamily: 'var(--eventus-font-mono)', fontSize: 10,
        letterSpacing: '0.14em', textTransform: 'uppercase',
        color: 'var(--eventus-fg-dim)', marginBottom: 10,
      }}>{label}</div>
      {children}
    </div>
  );
}

interface ModuleMetricsProps {
  ca: number;
  ce: number;
  instability: number;
  violations: ViolationItem[];
}

function ModuleMetrics({ ca, ce, instability, violations }: ModuleMetricsProps) {
  const barColor = instability > 0.7
    ? 'var(--eventus-danger)'
    : instability > 0.4
    ? 'var(--eventus-warn)'
    : 'var(--eventus-success)';

  const violationCount = violations.length;

  return (
    <div style={{ padding: '14px 22px', borderBottom: '1px solid var(--eventus-border)', display: 'flex', flexDirection: 'column', gap: 10 }}>
      {/* Coupling metrics row */}
      <div>
        <div style={{
          fontFamily: 'var(--eventus-font-mono)', fontSize: 10,
          letterSpacing: '0.14em', textTransform: 'uppercase',
          color: 'var(--eventus-fg-dim)', marginBottom: 8,
        }}>COUPLING</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <MetricChip label="Ca" value={ca} title="Afferent coupling — modules that depend on this one" />
          <MetricChip label="Ce" value={ce} title="Efferent coupling — modules this one depends on" />
          <div style={{ flex: 1 }}>
            <div style={{
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              fontFamily: 'var(--eventus-font-mono)', fontSize: 10, color: 'var(--eventus-fg-mute)',
              marginBottom: 4,
            }}>
              <span>I</span>
              <span style={{ color: barColor }}>{instability.toFixed(2)}</span>
            </div>
            <div style={{ height: 4, borderRadius: 999, background: 'var(--eventus-bg-inset)', overflow: 'hidden', border: '1px solid var(--eventus-border)' }}>
              <div style={{
                height: '100%',
                width: `${Math.round(instability * 100)}%`,
                background: barColor,
                borderRadius: 999,
                transition: 'width .3s ease',
              }} />
            </div>
          </div>
        </div>
      </div>

      {/* Violation summary */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 6,
        fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
        color: violationCount > 0 ? 'var(--eventus-danger)' : 'var(--eventus-success)',
      }}>
        {violationCount > 0 ? (
          <>
            <span style={{ opacity: 0.8 }}>▲</span>
            <span>{violationCount} violation{violationCount !== 1 ? 's' : ''}</span>
          </>
        ) : (
          <>
            <span>✓</span>
            <span>no violations</span>
          </>
        )}
      </div>
    </div>
  );
}

function MetricChip({ label, value, title }: { label: string; value: number; title: string }) {
  return (
    <div
      title={title}
      style={{
        display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2,
        padding: '5px 10px', borderRadius: 6,
        background: 'var(--eventus-bg-inset)', border: '1px solid var(--eventus-border)',
        cursor: 'default',
      }}
    >
      <span style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 9, color: 'var(--eventus-fg-dim)', letterSpacing: '0.08em' }}>{label}</span>
      <span style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 15, color: 'var(--eventus-fg)', lineHeight: 1 }}>{value}</span>
    </div>
  );
}

interface EventSectionProps {
  title: string;
  icon: string;
  events: RichEvent[];
  hiddenCouplingIds: Set<string>;
  onPick: (id: string) => void;
}

function EventSection({ title, icon, events, hiddenCouplingIds, onPick }: EventSectionProps) {
  return (
    <div style={{ padding: '14px 22px', borderBottom: '1px solid var(--eventus-border)' }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 6,
        fontFamily: 'var(--eventus-font-mono)', fontSize: 10,
        letterSpacing: '0.14em', textTransform: 'uppercase',
        color: 'var(--eventus-fg-dim)', marginBottom: 10, whiteSpace: 'nowrap',
      }}>
        <Icon d={icon} size={11} stroke="var(--eventus-fg-dim)" />
        <span>{title}</span>
      </div>
      {events.length === 0 ? (
        <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 11, color: 'var(--eventus-fg-dim)', fontStyle: 'italic' }}>none</div>
      ) : events.map(e => {
        const isHidden = hiddenCouplingIds.has(e.id);
        return (
          <button
            key={e.id}
            onClick={() => onPick(e.id)}
            title={isHidden ? `${e.simpleName} — undeclared / hidden coupling` : undefined}
            style={{
              display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%',
              background: 'transparent', border: 0, padding: '6px 0',
              fontFamily: 'var(--eventus-font-mono)', fontSize: 12,
              color: 'var(--eventus-fg)', cursor: 'pointer', textAlign: 'left',
              transition: 'color .12s',
            }}
            onMouseEnter={ev => { (ev.currentTarget as HTMLElement).style.color = 'var(--eventus-accent)'; }}
            onMouseLeave={ev => { (ev.currentTarget as HTMLElement).style.color = 'var(--eventus-fg)'; }}
          >
            <span style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0, overflow: 'hidden' }}>
              <span style={{
                width: 6, height: 6, borderRadius: '50%', flexShrink: 0,
                background: isHidden ? 'var(--eventus-warn)' : e.incomplete ? 'var(--eventus-danger)' : 'var(--eventus-accent)',
              }} />
              <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{e.simpleName}</span>
              {isHidden && (
                <span style={{
                  fontSize: 9, letterSpacing: '0.08em', textTransform: 'uppercase',
                  color: 'var(--eventus-warn)', border: '1px solid var(--eventus-warn)',
                  borderRadius: 3, padding: '1px 4px', flexShrink: 0,
                }}>hidden</span>
              )}
            </span>
            <span style={{ color: 'var(--eventus-fg-dim)', fontSize: 11, whiteSpace: 'nowrap', flexShrink: 0, marginLeft: 8 }}>
              {e.consumers.length === 0 ? '—' : `${e.consumers.length} cons.`}
            </span>
          </button>
        );
      })}
    </div>
  );
}

function ModuleChip({ id, onClick }: { id: string; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'inline-flex', alignItems: 'center', height: 26, padding: '0 10px', borderRadius: 6,
        background: 'var(--eventus-bg-raised)', border: '1px solid var(--eventus-border)',
        fontFamily: 'var(--eventus-font-mono)', fontSize: 12, color: 'var(--eventus-fg)',
        cursor: 'pointer', transition: 'border-color .12s',
      }}
      onMouseEnter={e => { (e.currentTarget as HTMLElement).style.borderColor = 'var(--eventus-accent)'; }}
      onMouseLeave={e => { (e.currentTarget as HTMLElement).style.borderColor = 'var(--eventus-border)'; }}
    >
      {displayId(id)}
    </button>
  );
}

interface RawJsonProps {
  path: string;
  obj: unknown;
}

function RawJson({ path, obj }: RawJsonProps) {
  const c = {
    string: 'oklch(76% 0.14 145)',
    number: 'oklch(78% 0.14 75)',
    bool:   'oklch(76% 0.13 230)',
    key:    'var(--eventus-fg)',
    punct:  'var(--eventus-fg-mute)',
  };

  function render(o: unknown, depth = 0): React.ReactNode {
    const indent = '  '.repeat(depth);
    if (o === null) return <span style={{ color: c.punct }}>null</span>;
    if (typeof o === 'string') return <span style={{ color: c.string }}>"{o}"</span>;
    if (typeof o === 'number') return <span style={{ color: c.number }}>{o}</span>;
    if (typeof o === 'boolean') return <span style={{ color: c.bool }}>{String(o)}</span>;
    if (Array.isArray(o)) {
      if (o.length === 0) return <span style={{ color: c.punct }}>[]</span>;
      return (
        <>
          <span style={{ color: c.punct }}>[</span>
          {o.map((v, i) => (
            <span key={i}>
              {'\n'}{indent}  {render(v, depth + 1)}{i < o.length - 1 ? <span style={{ color: c.punct }}>,</span> : ''}
            </span>
          ))}
          {'\n'}{indent}<span style={{ color: c.punct }}>]</span>
        </>
      );
    }
    if (typeof o === 'object' && o !== null) {
      const keys = Object.keys(o);
      return (
        <>
          <span style={{ color: c.punct }}>{'{'}</span>
          {keys.map((k, i) => (
            <span key={k}>
              {'\n'}{indent}  <span style={{ color: c.key }}>"{k}"</span>
              <span style={{ color: c.punct }}>: </span>
              {render((o as Record<string, unknown>)[k], depth + 1)}
              {i < keys.length - 1 ? <span style={{ color: c.punct }}>,</span> : ''}
            </span>
          ))}
          {'\n'}{indent}<span style={{ color: c.punct }}>{'}'}</span>
        </>
      );
    }
    return null;
  }

  return (
    <div style={{ padding: '14px 22px', flex: 1 }}>
      <div style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        fontFamily: 'var(--eventus-font-mono)', fontSize: 10,
        letterSpacing: '0.14em', textTransform: 'uppercase',
        color: 'var(--eventus-fg-dim)', marginBottom: 10,
      }}>
        <span>RAW · {path}</span>
        <button
          style={{
            background: 'transparent', border: 0, color: 'var(--eventus-fg-dim)',
            cursor: 'pointer', fontFamily: 'var(--eventus-font-mono)', fontSize: 10,
            letterSpacing: '0.14em',
          }}
          onClick={() => navigator.clipboard?.writeText(JSON.stringify(obj, null, 2))}
        >COPY</button>
      </div>
      <pre style={{
        margin: 0, padding: '12px 14px', borderRadius: 8,
        background: '#0d0d0d', border: '1px solid #1f1f1f',
        fontFamily: 'var(--eventus-font-mono)', fontSize: 11.5, lineHeight: 1.55,
        color: '#d4d4d4', whiteSpace: 'pre-wrap',
      }}>{render(obj)}</pre>
    </div>
  );
}
