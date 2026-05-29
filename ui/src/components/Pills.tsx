import type { ModuleStatus } from '../types';

const STATUS_COLOR: Record<ModuleStatus, string> = {
  HEALTHY: 'var(--eventus-success)',
  WARNING: 'var(--eventus-warn)',
  ERROR:   'var(--eventus-danger)',
};

interface StatusPillProps {
  status: ModuleStatus | 'INCOMPLETE';
  label?: string;
}

export function StatusPill({ status, label }: StatusPillProps) {
  const color = status === 'INCOMPLETE'
    ? 'var(--eventus-danger)'
    : STATUS_COLOR[status as ModuleStatus] ?? 'var(--eventus-fg-mute)';
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 6,
      height: 22, padding: '0 9px', borderRadius: 999,
      background: `color-mix(in oklch, ${color} 12%, transparent)`,
      color,
      border: `1px solid color-mix(in oklch, ${color} 28%, transparent)`,
      fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
      letterSpacing: '0.02em', whiteSpace: 'nowrap',
    }}>
      <span style={{ width: 6, height: 6, borderRadius: 999, background: color }} />
      {label ?? status}
    </span>
  );
}

interface NeutralPillProps {
  children: React.ReactNode;
}

export function NeutralPill({ children }: NeutralPillProps) {
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', height: 22, padding: '0 9px', borderRadius: 999,
      background: 'var(--eventus-bg-inset)', color: 'var(--eventus-fg-mute)',
      border: '1px solid var(--eventus-border)',
      fontFamily: 'var(--eventus-font-mono)', fontSize: 11, whiteSpace: 'nowrap',
    }}>{children}</span>
  );
}

export { STATUS_COLOR };
