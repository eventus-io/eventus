interface LockupProps {
  size?: number;
}

export function Lockup({ size = 22 }: LockupProps) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: size * 0.45 }}>
      <svg width={size * 1.1} height={size * 1.1} viewBox="0 0 24 24" fill="none">
        <path d="M7 12 L18 5"  stroke="var(--eventus-accent)" strokeWidth="1.5" strokeLinecap="round" opacity="0.65"/>
        <path d="M7 12 L18 19" stroke="var(--eventus-accent)" strokeWidth="1.5" strokeLinecap="round" opacity="0.65"/>
        <circle cx="18" cy="5"  r="2" fill="var(--eventus-accent)"/>
        <circle cx="18" cy="19" r="2" fill="var(--eventus-accent)"/>
        <circle cx="7"  cy="12" r="4.5" fill="var(--eventus-accent)"/>
        <circle cx="7"  cy="12" r="1.6" fill="var(--eventus-bg)"/>
      </svg>
      <span style={{
        fontFamily: 'var(--eventus-font-mono)',
        fontWeight: 500,
        fontSize: size,
        letterSpacing: '-0.04em',
        color: 'var(--eventus-fg)',
        lineHeight: 1,
      }}>eventus</span>
    </div>
  );
}
