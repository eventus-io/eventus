import { useState, useEffect } from 'react';
import { Lockup } from './Lockup';
import { Icon, ICON_REFRESH } from './Icon';
import { useBackends } from '../context/BackendsContext';
import { BackendsPanel } from './BackendsPanel';

interface TopBarProps {
  refreshKey: number;
  onRefresh: () => void;
  errors: Map<string, string>;
}

export function TopBar({ refreshKey, onRefresh, errors }: TopBarProps) {
  const [pulse, setPulse] = useState(false);
  const [showBackends, setShowBackends] = useState(false);
  const { backends } = useBackends();
  const enabledCount = backends.filter(b => b.enabled).length;
  const hasErrors = errors.size > 0;

  useEffect(() => {
    if (!refreshKey) return;
    setPulse(true);
    const t = setTimeout(() => setPulse(false), 800);
    return () => clearTimeout(t);
  }, [refreshKey]);

  return (
    <>
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

          {/* Backends button */}
          <button
            onClick={() => setShowBackends(true)}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 7,
              height: 26, padding: '0 10px', borderRadius: 6,
              background: 'transparent',
              border: `1px solid ${hasErrors ? 'color-mix(in oklch, var(--eventus-danger) 40%, var(--eventus-border))' : 'var(--eventus-border)'}`,
              color: 'var(--eventus-fg-mute)',
              fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
              cursor: 'pointer',
            }}
          >
            {/* Color dots for each enabled backend */}
            <span style={{ display: 'inline-flex', gap: 3, alignItems: 'center' }}>
              {backends.filter(b => b.enabled).map(b => (
                <span key={b.id} style={{
                  width: 6, height: 6, borderRadius: 999,
                  background: errors.has(b.id) ? 'var(--eventus-danger)' : b.color,
                }} />
              ))}
              {backends.filter(b => !b.enabled).map(b => (
                <span key={b.id} style={{
                  width: 6, height: 6, borderRadius: 999,
                  background: 'var(--eventus-bg-inset)',
                  border: '1px solid var(--eventus-border)',
                }} />
              ))}
            </span>
            <span>
              {enabledCount} backend{enabledCount !== 1 ? 's' : ''}
              {hasErrors && <span style={{ color: 'var(--eventus-danger)' }}> · {errors.size} unreachable</span>}
            </span>
          </button>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
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

      {showBackends && (
        <BackendsPanel errors={errors} onClose={() => setShowBackends(false)} />
      )}
    </>
  );
}
