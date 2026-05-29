import type { ActiveView } from '../types';

const TABS: { id: ActiveView; label: string; icon: string }[] = [
  { id: 'graph',        label: 'Graph',        icon: 'M7 10a3 3 0 1 1 6 0 3 3 0 0 1-6 0M3 4a2 2 0 1 1 4 0 2 2 0 0 1-4 0M13 4a2 2 0 1 1 4 0 2 2 0 0 1-4 0M13 16a2 2 0 1 1 4 0 2 2 0 0 1-4 0M6.5 5.5l2.5 3M13.5 5.5l-2.5 3M13.5 14.5l-2.5-3' },
  { id: 'impact',       label: 'Impact',       icon: 'M10 2.5L3 14h7l-1 7.5 11-13.5h-7L10 2.5z' },
  { id: 'violations',   label: 'Violations',   icon: 'M10 3l7.8 14a1 1 0 0 1-.87 1.5H3.07A1 1 0 0 1 2.2 17L10 3zM10 9v4M10 15h.01' },
  { id: 'drift',        label: 'Drift',        icon: 'M5 5h6M5 10h8M5 15h4M16 5l3 3-3 3M19 8h-5M16 13l3-3-3-3M19 10h-5' },
  { id: 'publications', label: 'Publications', icon: 'M4 5h16M4 9h12M4 13h8M4 17h10' },
];

interface NavTabsProps {
  active: ActiveView;
  onChange: (v: ActiveView) => void;
  incompleteCount: number;
  violationCount: number;
  driftBreachCount: number;
}

export function NavTabs({ active, onChange, incompleteCount, violationCount, driftBreachCount }: NavTabsProps) {
  const badge = (count: number, color: string) =>
    count > 0 ? (
      <span style={{
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        minWidth: 16, height: 16, borderRadius: 999, padding: '0 4px',
        background: color, color: '#0a0a0a',
        fontFamily: 'var(--eventus-font-mono)', fontSize: 9, fontWeight: 600,
        lineHeight: 1, flexShrink: 0,
      }}>{count}</span>
    ) : null;

  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 2,
      padding: '0 18px',
      borderBottom: '1px solid var(--eventus-border)',
      background: 'var(--eventus-bg-subtle)',
      flexShrink: 0,
      overflowX: 'auto',
    }}>
      {TABS.map(tab => {
        const isActive = tab.id === active;
        const badgeEl =
          tab.id === 'publications' ? badge(incompleteCount, 'var(--eventus-warn)') :
          tab.id === 'violations'   ? badge(violationCount, 'var(--eventus-danger)') :
          tab.id === 'drift'        ? badge(driftBreachCount, 'var(--eventus-danger)') :
          null;

        return (
          <button
            key={tab.id}
            onClick={() => onChange(tab.id)}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 6,
              padding: '8px 12px',
              background: 'transparent',
              border: 0,
              borderBottom: isActive ? '2px solid var(--eventus-accent)' : '2px solid transparent',
              color: isActive ? 'var(--eventus-fg)' : 'var(--eventus-fg-mute)',
              fontFamily: 'var(--eventus-font-mono)', fontSize: 12,
              cursor: 'pointer',
              whiteSpace: 'nowrap',
              transition: 'color .12s',
              marginBottom: -1,
            }}
            onMouseEnter={e => { if (!isActive) (e.currentTarget as HTMLElement).style.color = 'var(--eventus-fg)'; }}
            onMouseLeave={e => { if (!isActive) (e.currentTarget as HTMLElement).style.color = 'var(--eventus-fg-mute)'; }}
          >
            <TabIcon d={tab.icon} active={isActive} />
            {tab.label}
            {badgeEl}
          </button>
        );
      })}
    </div>
  );
}

function TabIcon({ d, active }: { d: string; active: boolean }) {
  return (
    <svg
      width={13} height={13}
      viewBox="0 0 20 20"
      fill="none"
      stroke={active ? 'var(--eventus-accent)' : 'currentColor'}
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      style={{ flexShrink: 0 }}
    >
      <path d={d} />
    </svg>
  );
}
