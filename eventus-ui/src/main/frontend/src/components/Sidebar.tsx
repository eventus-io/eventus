import type { Module, RichEvent, Publication, Selection } from '../types';
import { Icon, ICON_SEARCH } from './Icon';
import { STATUS_COLOR } from './Pills';

interface SidebarProps {
  filter: string;
  setFilter: (v: string) => void;
  selected: Selection | null;
  setSelected: (s: Selection | null) => void;
  modules: Module[];
  richEvents: RichEvent[];
  publications: Publication[];
}

export function Sidebar({
  filter, setFilter, selected, setSelected, modules, richEvents, publications,
}: SidebarProps) {
  const incomplete = publications.filter(p => p.status !== 'COMPLETED');

  return (
    <div style={{
      width: 248,
      background: 'var(--eventus-bg-subtle)',
      borderRight: '1px solid var(--eventus-border)',
      padding: '14px 8px',
      display: 'flex', flexDirection: 'column', gap: 1,
      flexShrink: 0, overflowY: 'auto',
    }}>
      <div style={{
        margin: '0 6px 6px', padding: '0 10px',
        borderRadius: 6,
        background: 'var(--eventus-bg-raised)',
        border: '1px solid var(--eventus-border)',
        display: 'flex', alignItems: 'center', gap: 8, height: 32,
      }}>
        <Icon d={ICON_SEARCH} size={13} stroke="var(--eventus-fg-dim)" />
        <input
          value={filter}
          onChange={e => setFilter(e.target.value)}
          placeholder="Filter modules, events…"
          style={{
            flex: 1, background: 'transparent', border: 0, outline: 'none',
            fontFamily: 'var(--eventus-font-mono)', fontSize: 12,
            color: 'var(--eventus-fg)',
          }}
        />
        {filter && (
          <button onClick={() => setFilter('')} style={{
            border: 0, background: 'transparent',
            color: 'var(--eventus-fg-dim)', cursor: 'pointer',
            fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
          }}>esc</button>
        )}
      </div>

      <SectionHeading label="Modules" count={modules.length} />
      {modules.length === 0 && <EmptyHint>no matches</EmptyHint>}
      {modules.map(m => (
        <SidebarItem
          key={m.id}
          label={m.id}
          sub={m.status === 'HEALTHY' ? `${m.beanCount}b` : m.status.toLowerCase()}
          subColor={STATUS_COLOR[m.status]}
          active={selected?.type === 'module' && selected.id === m.id}
          onClick={() => setSelected({ type: 'module', id: m.id })}
        />
      ))}

      <SectionHeading label="Events" count={richEvents.length} />
      {richEvents.length === 0 && <EmptyHint>no matches</EmptyHint>}
      {richEvents.map(e => (
        <SidebarItem
          key={e.id}
          label={e.simpleName}
          sub={e.incomplete ? '⚠' : 'pub'}
          subColor={e.incomplete ? 'var(--eventus-danger)' : 'var(--eventus-fg-dim)'}
          active={selected?.type === 'event' && selected.id === e.id}
          onClick={() => setSelected({ type: 'event', id: e.id })}
        />
      ))}

      <SectionHeading label="Publications" count={incomplete.length} />
      {incomplete.length === 0 && <EmptyHint>none incomplete</EmptyHint>}
      {incomplete.map((p, i) => (
        <SidebarItem
          key={i}
          label={p.eventType.split('.').pop() ?? p.eventType}
          sub={formatAge(p.publishedAt)}
          subColor="var(--eventus-warn)"
          active={false}
          onClick={() => setSelected({ type: 'event', id: p.eventType })}
        />
      ))}
    </div>
  );
}

function SectionHeading({ label, count }: { label: string; count: number }) {
  return (
    <div style={{
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      fontFamily: 'var(--eventus-font-mono)', fontSize: 10,
      letterSpacing: '0.14em', textTransform: 'uppercase',
      color: 'var(--eventus-fg-dim)',
      margin: '16px 12px 8px',
    }}>
      <span>{label}</span>
      <span style={{ letterSpacing: 0 }}>{count}</span>
    </div>
  );
}

interface SidebarItemProps {
  label: string;
  sub: string;
  subColor: string;
  active: boolean;
  onClick: () => void;
}

function SidebarItem({ label, sub, subColor, active, onClick }: SidebarItemProps) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        padding: '7px 10px', borderRadius: 6,
        background: active ? 'var(--eventus-bg-inset)' : 'transparent',
        color: active ? 'var(--eventus-fg)' : 'var(--eventus-fg-mute)',
        fontFamily: 'var(--eventus-font-mono)', fontSize: 12,
        border: 0,
        borderLeft: active ? '2px solid var(--eventus-accent)' : '2px solid transparent',
        paddingLeft: active ? 14 : 12,
        cursor: 'pointer', textAlign: 'left',
        transition: 'background .12s, color .12s',
        width: '100%',
      }}
      onMouseEnter={e => { if (!active) e.currentTarget.style.background = 'rgba(255,255,255,0.025)'; }}
      onMouseLeave={e => { if (!active) e.currentTarget.style.background = 'transparent'; }}
    >
      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{label}</span>
      <span style={{ color: subColor, fontSize: 10, flexShrink: 0, marginLeft: 8, whiteSpace: 'nowrap' }}>{sub}</span>
    </button>
  );
}

function EmptyHint({ children }: { children: React.ReactNode }) {
  return (
    <div style={{
      padding: '6px 12px',
      fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
      color: 'var(--eventus-fg-dim)', fontStyle: 'italic',
    }}>{children}</div>
  );
}

function formatAge(iso: string): string {
  const ms = Date.now() - new Date(iso).getTime();
  const h = Math.floor(ms / 3600000);
  const m = Math.floor((ms % 3600000) / 60000);
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}
