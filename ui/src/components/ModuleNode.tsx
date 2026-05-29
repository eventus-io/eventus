import type { Module, RichEvent, ViolationItem } from '../types';
import { STATUS_COLOR } from './Pills';

// SVG-unit width of the foreignObject that hosts this card.
// The HTML content fills width:100% of that viewport.
export const NODE_W = 164;

// Collapsed card height in CSS px. The header row is always this tall.
export const COLLAPSED_H = 30;

// Max-height of the detail panel in CSS px (below the header).
const DETAIL_MAX_H = 205;

// foreignObject height in SVG units — kept generously large so the HTML detail
// panel is never clipped even when the SVG is scaled. The transparent area
// below the content is harmless because the foreignObject has pointerEvents:none.
export const FO_HEIGHT = 290;

interface ModuleNodeProps {
  module: Module;
  isExpanded: boolean;
  isSelected: boolean;
  dim: boolean;
  publishedEvents: RichEvent[];
  subscribedEvents: RichEvent[];
  hiddenCouplingEventIds: Set<string>;
  violations: ViolationItem[];
  ca: number;
  ce: number;
  onToggle: () => void;
  onHoverEnter: () => void;
  onHoverLeave: () => void;
}

export function ModuleNode({
  module: m,
  isExpanded,
  isSelected,
  dim,
  publishedEvents,
  subscribedEvents,
  hiddenCouplingEventIds,
  violations,
  ca,
  ce,
  onToggle,
  onHoverEnter,
  onHoverLeave,
}: ModuleNodeProps) {
  const statusColor = STATUS_COLOR[m.status] ?? 'var(--eventus-fg-dim)';
  const instability = ca + ce === 0 ? 0 : ce / (ca + ce);
  const violationCount = violations.length;

  const headerBorderRadius = isExpanded
    ? 'var(--eventus-radius-md) var(--eventus-radius-md) 0 0'
    : 'var(--eventus-radius-md)';

  const selectionRing = isSelected
    ? `0 0 0 1.5px var(--eventus-accent), 0 0 0 3px color-mix(in oklch, var(--eventus-accent) 20%, transparent)`
    : undefined;

  return (
    // width:100% fills the foreignObject viewport regardless of SVG scale
    <div
      style={{
        width: '100%',
        opacity: dim ? 0.25 : 1,
        transition: 'opacity .15s',
        fontFamily: 'var(--eventus-font-mono)',
        userSelect: 'none',
        pointerEvents: 'auto',
      }}
    >
      {/* ── Header row (always visible) ───────────────────────────── */}
      <div
        onClick={e => { e.stopPropagation(); onToggle(); }}
        onMouseEnter={onHoverEnter}
        onMouseLeave={onHoverLeave}
        style={{
          height: COLLAPSED_H,
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          padding: '0 8px',
          borderRadius: headerBorderRadius,
          background: 'var(--eventus-node-module-fill)',
          border: `1.5px solid ${statusColor}`,
          borderBottom: isExpanded ? `1px solid var(--eventus-border)` : `1.5px solid ${statusColor}`,
          boxSizing: 'border-box',
          cursor: 'pointer',
          boxShadow: selectionRing,
          transition: 'box-shadow .15s',
          overflow: 'hidden',
        }}
      >
        {/* Health dot */}
        <span style={{
          width: 6,
          height: 6,
          borderRadius: '50%',
          background: statusColor,
          flexShrink: 0,
        }} />

        {/* Module name */}
        <span style={{
          flex: 1,
          color: 'var(--eventus-fg)',
          fontSize: 11,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
          lineHeight: 1,
        }}>
          {m.id}
        </span>

        {/* Chevron — rotates when expanded */}
        <svg
          width={9} height={9}
          viewBox="0 0 9 9"
          fill="none"
          stroke="var(--eventus-fg-dim)"
          strokeWidth={1.5}
          strokeLinecap="round"
          style={{
            flexShrink: 0,
            transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)',
            transition: 'transform .22s cubic-bezier(0.4, 0, 0.2, 1)',
          }}
        >
          <path d="M1.5 3L4.5 6L7.5 3" />
        </svg>
      </div>

      {/* ── Expanded detail panel ─────────────────────────────────── */}
      <div
        style={{
          overflow: 'hidden',
          maxHeight: isExpanded ? `${DETAIL_MAX_H}px` : '0',
          transition: 'max-height .22s cubic-bezier(0.4, 0, 0.2, 1)',
          // Match header left/right border; bottom border and radius come from inner div
          borderLeft: `1.5px solid ${statusColor}`,
          borderRight: `1.5px solid ${statusColor}`,
          borderBottom: `1.5px solid ${statusColor}`,
          borderRadius: `0 0 var(--eventus-radius-md) var(--eventus-radius-md)`,
          background: 'var(--eventus-node-module-fill)',
          boxSizing: 'border-box',
        }}
      >
        <div style={{ padding: '7px 8px 8px' }}>

          {/* Published events */}
          <SectionLabel>↑ publishes</SectionLabel>
          <EventPills
            events={publishedEvents}
            color="var(--eventus-accent)"
            hiddenCouplingIds={new Set()}
          />

          {/* Subscribed events */}
          <SectionLabel style={{ marginTop: 5 }}>↓ subscribes</SectionLabel>
          <EventPills
            events={subscribedEvents}
            color="var(--eventus-info)"
            hiddenCouplingIds={hiddenCouplingEventIds}
          />

          {/* Metrics row */}
          <div style={{
            marginTop: 7,
            paddingTop: 6,
            borderTop: '1px solid var(--eventus-border)',
          }}>
            {/* Ca / Ce / Instability bar */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: 4,
              fontSize: 10,
              color: 'var(--eventus-fg-mute)',
              marginBottom: 4,
            }}>
              <span>Ca&thinsp;{ca}</span>
              <Dot />
              <span>Ce&thinsp;{ce}</span>
              <Dot />
              <span>I&thinsp;{instability.toFixed(2)}</span>
              <InstabilityBar value={instability} />
            </div>

            {/* Violation summary */}
            <div style={{
              fontSize: 10,
              color: violationCount > 0 ? 'var(--eventus-danger)' : 'var(--eventus-success)',
              display: 'flex',
              alignItems: 'center',
              gap: 4,
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
        </div>
      </div>
    </div>
  );
}

// ── Sub-components ────────────────────────────────────────────────────────────

function SectionLabel({ children, style }: { children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <div style={{
      fontSize: 9,
      letterSpacing: '0.1em',
      textTransform: 'uppercase' as const,
      color: 'var(--eventus-fg-dim)',
      marginBottom: 3,
      lineHeight: 1,
      ...style,
    }}>
      {children}
    </div>
  );
}

interface EventPillsProps {
  events: RichEvent[];
  color: string;
  hiddenCouplingIds: Set<string>;
}

const MAX_PILLS = 3;

function EventPills({ events, color, hiddenCouplingIds }: EventPillsProps) {
  if (events.length === 0) {
    return (
      <div style={{
        fontSize: 10,
        color: 'var(--eventus-fg-dim)',
        fontStyle: 'italic',
        marginBottom: 2,
        lineHeight: 1.4,
      }}>
        none
      </div>
    );
  }

  const shown = events.slice(0, MAX_PILLS);
  const overflow = events.length - MAX_PILLS;

  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 3, marginBottom: 2 }}>
      {shown.map(e => {
        const isHidden = hiddenCouplingIds.has(e.id);
        const c = isHidden ? 'var(--eventus-warn)' : color;
        return (
          <span
            key={e.id}
            title={isHidden ? `${e.simpleName} — undeclared / hidden coupling` : e.simpleName}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 3,
              height: 17,
              padding: '0 5px',
              borderRadius: 'var(--eventus-radius-sm)',
              background: `color-mix(in oklch, ${c} 14%, transparent)`,
              border: `1px solid color-mix(in oklch, ${c} 30%, transparent)`,
              color: c,
              fontSize: 10,
              maxWidth: '100%',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              boxSizing: 'border-box',
            }}
          >
            {isHidden && (
              <svg width={7} height={7} viewBox="0 0 7 7" fill="var(--eventus-warn)" style={{ flexShrink: 0 }}>
                <path d="M3.5 0L7 7H0L3.5 0z" />
              </svg>
            )}
            {e.simpleName}
          </span>
        );
      })}
      {overflow > 0 && (
        <span style={{
          display: 'inline-flex',
          alignItems: 'center',
          height: 17,
          padding: '0 5px',
          borderRadius: 'var(--eventus-radius-sm)',
          background: 'var(--eventus-bg-inset)',
          border: '1px solid var(--eventus-border)',
          color: 'var(--eventus-fg-dim)',
          fontSize: 10,
        }}>
          +{overflow}
        </span>
      )}
    </div>
  );
}

function InstabilityBar({ value }: { value: number }) {
  const barColor = value > 0.7
    ? 'var(--eventus-danger)'
    : value > 0.4
    ? 'var(--eventus-warn)'
    : 'var(--eventus-success)';

  return (
    <div style={{
      flex: 1,
      height: 3,
      borderRadius: 999,
      background: 'var(--eventus-bg-inset)',
      overflow: 'hidden',
      border: '1px solid var(--eventus-border)',
    }}>
      <div style={{
        height: '100%',
        width: `${Math.round(value * 100)}%`,
        background: barColor,
        borderRadius: 999,
        transition: 'width .3s ease',
      }} />
    </div>
  );
}

function Dot() {
  return (
    <span style={{ color: 'var(--eventus-fg-faint)', lineHeight: 1 }}>·</span>
  );
}
