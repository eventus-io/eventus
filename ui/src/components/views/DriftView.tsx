import { useState, useEffect, useCallback } from 'react';
import type { DriftReport, DriftItem, DriftSeverity } from '../../types';
import { fetchDrift, postBaseline } from '../../client';
import { useBackends } from '../../context/BackendsContext';

const SEV_COLOR: Record<DriftSeverity, string> = {
  BREAKING: 'var(--eventus-danger)',
  MODERATE: 'var(--eventus-warn)',
  MINOR:    'var(--eventus-info)',
};

const TYPE_LABELS: Record<string, string> = {
  MODULE_ADDED:       'Module Added',
  MODULE_REMOVED:     'Module Removed',
  EVENT_ADDED:        'Event Added',
  EVENT_REMOVED:      'Event Removed',
  LISTENER_ADDED:     'Listener Added',
  LISTENER_REMOVED:   'Listener Removed',
  PUBLISHER_CHANGED:  'Publisher Changed',
};

export function DriftView() {
  const { backends } = useBackends();
  const [report, setReport]   = useState<DriftReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);
  const [saving, setSaving]   = useState(false);
  const [saved, setSaved]     = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    const enabled = backends.filter(b => b.enabled);
    // Merge drift reports: sum counts, concatenate drifts (namespaced)
    Promise.all(enabled.map(b => fetchDrift(b.url).then(r => ({ b, r }))))
      .then(results => {
        const merged: DriftReport = {
          drifts: results.flatMap(({ b, r }) =>
            r.drifts.map(d => ({ ...d, id: `${b.id}::${d.id}` }))
          ),
          totalDrifts: results.reduce((s, { r }) => s + r.totalDrifts, 0),
          breachingCount: results.reduce((s, { r }) => s + r.breachingCount, 0),
          comparedAt: Math.max(...results.map(({ r }) => r.comparedAt)),
        };
        setReport(merged);
      })
      .catch(e => setError(String(e)))
      .finally(() => setLoading(false));
  }, [backends]);

  useEffect(() => { load(); }, [load]);

  const handleSaveBaseline = async () => {
    setSaving(true);
    try {
      const enabled = backends.filter(b => b.enabled);
      await Promise.all(enabled.map(b => postBaseline(b.url)));
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
      load();
    } catch (e) {
      setError(String(e));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <Centered><Spinner /></Centered>;
  if (error)   return <Centered><ErrorMsg msg={error} /></Centered>;
  if (!report) return null;

  const hasBaseline = report.totalDrifts > 0 || report.comparedAt > 0;

  return (
    <div style={{ flex: 1, overflowY: 'auto', padding: '28px 36px', maxWidth: 800 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 28 }}>
        <div>
          <h2 style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 20, color: 'var(--eventus-fg)', margin: '0 0 6px', letterSpacing: '-0.02em' }}>Drift Detection</h2>
          <div style={{ fontFamily: 'var(--eventus-font-sans)', fontSize: 13, color: 'var(--eventus-fg-mute)' }}>
            Topology changes since the last saved baseline.
            {report.comparedAt > 0 && (
              <span style={{ marginLeft: 8, color: 'var(--eventus-fg-dim)', fontFamily: 'var(--eventus-font-mono)', fontSize: 11 }}>
                Baseline from {new Date(report.comparedAt).toLocaleString()}
              </span>
            )}
          </div>
        </div>
        <button
          onClick={handleSaveBaseline}
          disabled={saving}
          style={{
            display: 'inline-flex', alignItems: 'center', gap: 6,
            height: 32, padding: '0 14px', borderRadius: 6,
            background: saved ? 'color-mix(in oklch, var(--eventus-success) 14%, transparent)' : 'var(--eventus-accent)',
            border: saved ? '1px solid color-mix(in oklch, var(--eventus-success) 32%, transparent)' : '0',
            color: saved ? 'var(--eventus-success)' : 'var(--eventus-accent-ink)',
            fontFamily: 'var(--eventus-font-mono)', fontSize: 12,
            cursor: saving ? 'default' : 'pointer',
            opacity: saving ? 0.7 : 1,
            flexShrink: 0,
            transition: 'background .3s, color .3s',
          }}
        >
          {saved ? '✓ Baseline saved' : saving ? 'Saving…' : 'Save Baseline'}
        </button>
      </div>

      {!hasBaseline ? (
        <EmptyState
          icon="◎"
          title="No baseline saved yet"
          subtitle="Save a baseline to start tracking topology drift. Future changes (modules added/removed, events changed) will appear here."
        />
      ) : report.drifts.length === 0 ? (
        <EmptyState
          icon="✓"
          title="No drift detected"
          subtitle="The current topology matches the baseline. All modules and events are as expected."
        />
      ) : (
        <>
          <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
            {report.breachingCount > 0 && <DriftBadge count={report.breachingCount} sev="BREAKING" />}
            {report.drifts.filter(d => d.severity === 'MODERATE').length > 0 && (
              <DriftBadge count={report.drifts.filter(d => d.severity === 'MODERATE').length} sev="MODERATE" />
            )}
            {report.drifts.filter(d => d.severity === 'MINOR').length > 0 && (
              <DriftBadge count={report.drifts.filter(d => d.severity === 'MINOR').length} sev="MINOR" />
            )}
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {report.drifts.map(d => <DriftCard key={d.id} d={d} />)}
          </div>
        </>
      )}
    </div>
  );
}

function DriftCard({ d }: { d: DriftItem }) {
  const color = SEV_COLOR[d.severity];
  return (
    <div style={{
      padding: '16px 20px', borderRadius: 8,
      border: `1px solid color-mix(in oklch, ${color} 28%, var(--eventus-border))`,
      background: 'var(--eventus-bg-raised)',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16, marginBottom: 8 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ width: 8, height: 8, borderRadius: 999, background: color, flexShrink: 0 }} />
          <span style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 13, color: 'var(--eventus-fg)' }}>{d.title}</span>
        </div>
        <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
          <Tag text={TYPE_LABELS[d.type] ?? d.type} />
          <Tag text={d.severity} color={color} />
        </div>
      </div>
      <div style={{ fontFamily: 'var(--eventus-font-sans)', fontSize: 13, color: 'var(--eventus-fg-mute)', lineHeight: 1.55, marginBottom: 10 }}>
        {d.description}
      </div>
      {d.affectedItemName && (
        <span style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 11, padding: '3px 8px', borderRadius: 6, border: '1px solid var(--eventus-border)', background: 'var(--eventus-bg-inset)', color: 'var(--eventus-fg-mute)' }}>
          {d.affectedItemName}
        </span>
      )}
    </div>
  );
}

function DriftBadge({ count, sev }: { count: number; sev: DriftSeverity }) {
  const c = SEV_COLOR[sev];
  return (
    <span style={{
      fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
      padding: '3px 9px', borderRadius: 999,
      background: `color-mix(in oklch, ${c} 12%, transparent)`,
      color: c, border: `1px solid color-mix(in oklch, ${c} 28%, transparent)`,
    }}>{count} {sev.toLowerCase()}</span>
  );
}

function Tag({ text, color }: { text: string; color?: string }) {
  return (
    <span style={{
      fontFamily: 'var(--eventus-font-mono)', fontSize: 10, letterSpacing: '0.05em',
      padding: '2px 7px', borderRadius: 4,
      border: '1px solid var(--eventus-border)', background: 'var(--eventus-bg-inset)',
      color: color ?? 'var(--eventus-fg-mute)',
    }}>{text}</span>
  );
}

function EmptyState({ icon, title, subtitle }: { icon: string; title: string; subtitle: string }) {
  return (
    <div style={{ paddingTop: 40, textAlign: 'center' }}>
      <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 28, marginBottom: 14, color: 'var(--eventus-fg-dim)' }}>{icon}</div>
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
