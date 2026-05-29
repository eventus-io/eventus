import { useState } from 'react';
import type { Publication, PublicationStatus } from '../../types';

const STATUS_COLOR: Record<PublicationStatus, string> = {
  COMPLETED:  'var(--eventus-success)',
  INCOMPLETE: 'var(--eventus-danger)',
  STALE:      'var(--eventus-warn)',
};

interface PublicationsViewProps {
  publications: Publication[];
}

type Filter = 'all' | 'incomplete' | 'stale';

export function PublicationsView({ publications }: PublicationsViewProps) {
  const [filter, setFilter] = useState<Filter>('all');

  const visible = publications.filter(p => {
    if (filter === 'all')        return true;
    if (filter === 'incomplete') return p.status === 'INCOMPLETE';
    if (filter === 'stale')      return p.status === 'STALE';
    return true;
  });

  const incomplete = publications.filter(p => p.status === 'INCOMPLETE').length;
  const stale      = publications.filter(p => p.status === 'STALE').length;

  return (
    <div style={{ flex: 1, overflowY: 'auto', padding: '28px 36px', maxWidth: 900 }}>
      <div style={{ marginBottom: 24 }}>
        <h2 style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 20, color: 'var(--eventus-fg)', margin: '0 0 6px', letterSpacing: '-0.02em' }}>Publication Log</h2>
        <div style={{ fontFamily: 'var(--eventus-font-sans)', fontSize: 13, color: 'var(--eventus-fg-mute)' }}>
          Transactional event publications — Spring Modulith tracks delivery of each event to every listener.
        </div>
      </div>

      {/* Filter tabs */}
      <div style={{ display: 'flex', gap: 6, marginBottom: 20 }}>
        {(['all', 'incomplete', 'stale'] as Filter[]).map(f => {
          const count = f === 'all' ? publications.length : f === 'incomplete' ? incomplete : stale;
          const isActive = filter === f;
          return (
            <button
              key={f}
              onClick={() => setFilter(f)}
              style={{
                display: 'inline-flex', alignItems: 'center', gap: 6,
                height: 28, padding: '0 12px', borderRadius: 6,
                background: isActive ? 'var(--eventus-bg-raised)' : 'transparent',
                border: isActive ? '1px solid var(--eventus-border)' : '1px solid transparent',
                color: isActive ? 'var(--eventus-fg)' : 'var(--eventus-fg-mute)',
                fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
                cursor: 'pointer',
              }}
            >
              {f === 'all' ? 'All' : f === 'incomplete' ? 'Incomplete' : 'Stale'}
              <span style={{
                fontFamily: 'var(--eventus-font-mono)', fontSize: 10,
                padding: '1px 6px', borderRadius: 999,
                background: 'var(--eventus-bg-inset)', color: 'var(--eventus-fg-dim)',
              }}>{count}</span>
            </button>
          );
        })}
      </div>

      {visible.length === 0 ? (
        <div style={{ paddingTop: 40, textAlign: 'center' }}>
          <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 28, marginBottom: 14, color: 'var(--eventus-fg-dim)' }}>✓</div>
          <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 13, color: 'var(--eventus-fg)', marginBottom: 8 }}>
            {filter === 'all' ? 'No publications recorded' : `No ${filter} publications`}
          </div>
          <div style={{ fontFamily: 'var(--eventus-font-sans)', fontSize: 13, color: 'var(--eventus-fg-mute)', maxWidth: 380, margin: '0 auto', lineHeight: 1.6 }}>
            {filter === 'all'
              ? 'Spring Modulith event publications will appear here once events are dispatched.'
              : `All publications have been delivered successfully.`}
          </div>
        </div>
      ) : (
        <div style={{
          borderRadius: 8, border: '1px solid var(--eventus-border)',
          overflow: 'hidden',
        }}>
          {/* Header row */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: '1fr 140px 120px 150px 100px',
            padding: '8px 16px',
            background: 'var(--eventus-bg-raised)',
            borderBottom: '1px solid var(--eventus-border)',
            fontFamily: 'var(--eventus-font-mono)', fontSize: 10, letterSpacing: '0.12em', textTransform: 'uppercase',
            color: 'var(--eventus-fg-dim)',
          }}>
            <span>Event Type</span>
            <span>Module</span>
            <span>Listener</span>
            <span>Published At</span>
            <span>Status</span>
          </div>
          {visible.map((p, i) => (
            <div
              key={p.id}
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 140px 120px 150px 100px',
                padding: '10px 16px',
                borderBottom: i < visible.length - 1 ? '1px solid var(--eventus-border)' : 'none',
                background: i % 2 === 0 ? 'transparent' : 'color-mix(in oklch, var(--eventus-bg-subtle) 60%, transparent)',
                alignItems: 'center',
              }}
            >
              <div style={{ overflow: 'hidden' }}>
                <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 12, color: 'var(--eventus-fg)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {p.eventType.split('.').pop() ?? p.eventType}
                </div>
                <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 10, color: 'var(--eventus-fg-dim)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginTop: 2 }}>
                  {p.eventType}
                </div>
              </div>
              <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 12, color: 'var(--eventus-fg-mute)' }}>
                {p.moduleId ?? '—'}
              </div>
              <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 11, color: 'var(--eventus-fg-dim)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {p.listenerName ? p.listenerName.split('.').pop() ?? p.listenerName : '—'}
              </div>
              <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 11, color: 'var(--eventus-fg-dim)' }}>
                {p.publishedAt ? formatDateTime(p.publishedAt) : '—'}
              </div>
              <div>
                <StatusBadge status={p.status} />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function StatusBadge({ status }: { status: PublicationStatus }) {
  const color = STATUS_COLOR[status];
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 5,
      height: 20, padding: '0 8px', borderRadius: 999,
      background: `color-mix(in oklch, ${color} 12%, transparent)`,
      color, border: `1px solid color-mix(in oklch, ${color} 28%, transparent)`,
      fontFamily: 'var(--eventus-font-mono)', fontSize: 10, whiteSpace: 'nowrap',
    }}>
      <span style={{ width: 5, height: 5, borderRadius: 999, background: color }} />
      {status}
    </span>
  );
}

function formatDateTime(iso: string): string {
  try {
    const d = new Date(iso);
    return d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  } catch {
    return iso;
  }
}
