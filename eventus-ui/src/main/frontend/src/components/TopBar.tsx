import { useState, useEffect } from 'react';
import { Lockup } from './Lockup';
import { Icon, ICON_REFRESH } from './Icon';

interface TopBarProps {
  refreshKey: number;
  onRefresh: () => void;
}

export function TopBar({ refreshKey, onRefresh }: TopBarProps) {
  const [pulse, setPulse] = useState(false);

  useEffect(() => {
    if (!refreshKey) return;
    setPulse(true);
    const t = setTimeout(() => setPulse(false), 800);
    return () => clearTimeout(t);
  }, [refreshKey]);

  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '12px 22px',
      borderBottom: '1px solid var(--eventus-border)',
      background: 'var(--eventus-bg-subtle)',
      flexShrink: 0,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
        <Lockup size={20} />
        <div style={{ width: 1, height: 18, background: 'var(--eventus-border)' }} />
        <div style={{
          display: 'flex', alignItems: 'center', gap: 6,
          fontFamily: 'var(--eventus-font-mono)', fontSize: 12,
          color: 'var(--eventus-fg-mute)',
        }}>
          <span>localhost:8080</span>
          <span style={{ color: 'var(--eventus-fg-dim)' }}>/</span>
          <span>actuator</span>
          <span style={{ color: 'var(--eventus-fg-dim)' }}>/</span>
          <span style={{ color: 'var(--eventus-fg)' }}>eventus</span>
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <Chip>zoom 100%</Chip>
        <Chip>layout · auto</Chip>
        <button
          onClick={onRefresh}
          style={{
            display: 'inline-flex', alignItems: 'center', gap: 6,
            height: 26, padding: '0 9px', borderRadius: 6,
            background: 'transparent',
            border: '1px solid var(--eventus-border)',
            color: 'var(--eventus-fg-mute)',
            fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
            cursor: 'pointer',
          }}
        >
          <Icon d={ICON_REFRESH} size={12} />
          refresh
        </button>
        <span style={{
          display: 'inline-flex', alignItems: 'center', gap: 6,
          fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
          color: 'var(--eventus-success)',
          padding: '4px 8px', borderRadius: 6,
          border: '1px solid color-mix(in oklch, var(--eventus-success) 28%, transparent)',
          background: 'color-mix(in oklch, var(--eventus-success) 10%, transparent)',
        }}>
          <span style={{
            width: 6, height: 6, borderRadius: 999,
            background: 'var(--eventus-success)',
            boxShadow: pulse
              ? '0 0 0 4px color-mix(in oklch, var(--eventus-success) 30%, transparent)'
              : 'none',
            transition: 'box-shadow .4s ease-out',
          }} />
          live · 2s
        </span>
      </div>
    </div>
  );
}

function Chip({ children }: { children: React.ReactNode }) {
  return (
    <span style={{
      fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
      color: 'var(--eventus-fg-mute)',
      padding: '4px 8px', border: '1px solid var(--eventus-border)',
      borderRadius: 6, background: 'var(--eventus-bg-raised)',
      whiteSpace: 'nowrap',
    }}>{children}</span>
  );
}
