import { useState, useEffect } from 'react';
import type { ViolationItem, ViolationSeverity } from '../../types';
import { fetchViolations } from '../../client';
import { useBackends } from '../../context/BackendsContext';

const SEV_COLOR: Record<ViolationSeverity, string> = {
  ERROR:   'var(--eventus-danger)',
  WARNING: 'var(--eventus-warn)',
  INFO:    'var(--eventus-info)',
};

const TYPE_LABELS: Record<string, string> = {
  CIRCULAR_EVENT_DEPENDENCY: 'Circular Dependency',
  HIDDEN_COUPLING:           'Hidden Coupling',
  UNUSED_EVENT:              'Unused Event',
  FAILING_LISTENER:          'Failing Listener',
  STALE_PUBLICATION:         'Stale Publication',
};

export function ViolationsView() {
  const { backends } = useBackends();
  const [items, setItems]     = useState<ViolationItem[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    const enabled = backends.filter(b => b.enabled);
    Promise.all(
      enabled.map(b =>
        fetchViolations(b.url).then(vs =>
          vs.map(v => ({ ...v, id: `${b.id}::${v.id}` }))
        )
      )
    )
      .then(results => setItems(results.flat()))
      .catch(e => setError(String(e)))
      .finally(() => setLoading(false));
  }, [backends]);

  if (loading) return <Centered><Spinner /></Centered>;
  if (error)   return <Centered><ErrorMsg msg={error} /></Centered>;
  if (!items)  return null;

  const errors   = items.filter(v => v.severity === 'ERROR');
  const warnings = items.filter(v => v.severity === 'WARNING');
  const infos    = items.filter(v => v.severity === 'INFO');

  return (
    <div style={{ flex: 1, overflowY: 'auto', padding: '28px 36px', maxWidth: 800 }}>
      <PageHeader
        title="Violations"
        subtitle="Hidden couplings, unused events, and architectural issues detected in the topology."
      >
        {items.length > 0 && (
          <div style={{ display: 'flex', gap: 8 }}>
            {errors.length   > 0 && <SevBadge count={errors.length}   sev="ERROR"   />}
            {warnings.length > 0 && <SevBadge count={warnings.length} sev="WARNING" />}
            {infos.length    > 0 && <SevBadge count={infos.length}    sev="INFO"    />}
          </div>
        )}
      </PageHeader>

      {items.length === 0 ? (
        <EmptyState icon="✓" title="No violations detected" subtitle="The topology is clean — no hidden couplings, unused events, or circular dependencies found." />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {items.map(v => <ViolationCard key={v.id} v={v} />)}
        </div>
      )}
    </div>
  );
}

function ViolationCard({ v }: { v: ViolationItem }) {
  const color = SEV_COLOR[v.severity];
  return (
    <div style={{
      padding: '16px 20px', borderRadius: 8,
      border: `1px solid color-mix(in oklch, ${color} 28%, var(--eventus-border))`,
      background: 'var(--eventus-bg-raised)',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16, marginBottom: 8 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <SevDot color={color} />
          <span style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 13, color: 'var(--eventus-fg)' }}>{v.title}</span>
        </div>
        <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
          <Tag text={TYPE_LABELS[v.type] ?? v.type} />
          <Tag text={v.severity} color={color} />
        </div>
      </div>
      <div style={{ fontFamily: 'var(--eventus-font-sans)', fontSize: 13, color: 'var(--eventus-fg-mute)', lineHeight: 1.55, marginBottom: 12 }}>
        {v.description}
      </div>
      {(v.affectedModuleIds.length > 0 || v.affectedEventIds.length > 0) && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {v.affectedModuleIds.map(id => <Chip key={id} text={id} type="module" />)}
          {v.affectedEventIds.map(id  => <Chip key={id} text={id.split('.').pop() ?? id} type="event" />)}
        </div>
      )}
    </div>
  );
}

function SevBadge({ count, sev }: { count: number; sev: ViolationSeverity }) {
  const c = SEV_COLOR[sev];
  return (
    <span style={{
      fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
      padding: '3px 9px', borderRadius: 999,
      background: `color-mix(in oklch, ${c} 12%, transparent)`,
      color: c, border: `1px solid color-mix(in oklch, ${c} 28%, transparent)`,
    }}>{count} {sev.toLowerCase()}{count !== 1 ? 's' : ''}</span>
  );
}

function SevDot({ color }: { color: string }) {
  return <span style={{ width: 8, height: 8, borderRadius: 999, background: color, flexShrink: 0, marginTop: 2 }} />;
}

function Tag({ text, color }: { text: string; color?: string }) {
  return (
    <span style={{
      fontFamily: 'var(--eventus-font-mono)', fontSize: 10, letterSpacing: '0.05em',
      padding: '2px 7px', borderRadius: 4,
      border: '1px solid var(--eventus-border)',
      background: 'var(--eventus-bg-inset)',
      color: color ?? 'var(--eventus-fg-mute)',
    }}>{text}</span>
  );
}

function Chip({ text, type }: { text: string; type: 'module' | 'event' }) {
  return (
    <span style={{
      fontFamily: 'var(--eventus-font-mono)', fontSize: 11, padding: '3px 8px', borderRadius: 6,
      border: '1px solid var(--eventus-border)', background: 'var(--eventus-bg-inset)',
      color: type === 'event' ? 'var(--eventus-accent)' : 'var(--eventus-fg-mute)',
    }}>{text}</span>
  );
}

function PageHeader({ title, subtitle, children }: { title: string; subtitle: string; children?: React.ReactNode }) {
  return (
    <div style={{ marginBottom: 28 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16, marginBottom: 6 }}>
        <h2 style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 20, color: 'var(--eventus-fg)', margin: 0, letterSpacing: '-0.02em' }}>{title}</h2>
        {children}
      </div>
      <div style={{ fontFamily: 'var(--eventus-font-sans)', fontSize: 13, color: 'var(--eventus-fg-mute)' }}>{subtitle}</div>
    </div>
  );
}

function EmptyState({ icon, title, subtitle }: { icon: string; title: string; subtitle: string }) {
  return (
    <div style={{ paddingTop: 40, textAlign: 'center' }}>
      <div style={{ fontSize: 32, marginBottom: 14, opacity: 0.5 }}>{icon}</div>
      <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 13, color: 'var(--eventus-fg)', marginBottom: 8 }}>{title}</div>
      <div style={{ fontFamily: 'var(--eventus-font-sans)', fontSize: 13, color: 'var(--eventus-fg-mute)', maxWidth: 380, margin: '0 auto', lineHeight: 1.6 }}>{subtitle}</div>
    </div>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <div style={{ display: 'flex', flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32 }}>{children}</div>;
}

function Spinner() {
  return <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 12, color: 'var(--eventus-fg-dim)' }}>loading…</div>;
}

function ErrorMsg({ msg }: { msg: string }) {
  return <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 12, color: 'var(--eventus-danger)' }}>{msg}</div>;
}
