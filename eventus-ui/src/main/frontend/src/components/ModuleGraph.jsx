import { useState } from 'react'

const STATUS_COLOR = { HEALTHY: '#48bb78', WARNING: '#ed8936', ERROR: '#fc8181', UNKNOWN: '#718096' }
const W = 960, H = 520, NODE_R = 40

function layoutNodes(modules) {
  const count = modules.length
  return modules.map((m, i) => {
    const angle = (2 * Math.PI * i) / Math.max(count, 1) - Math.PI / 2
    const rx = count === 1 ? 0 : W / 2 - NODE_R - 60
    const ry = count === 1 ? 0 : H / 2 - NODE_R - 60
    return { ...m, x: W / 2 + rx * Math.cos(angle), y: H / 2 + ry * Math.sin(angle) }
  })
}

// Derive publisher→listener flow edges by joining through event.publisherModuleId
function buildFlowEdges(events, edges) {
  const listeners = edges.filter(e => e.edgeType === 'LISTENS_TO' && e.toModuleId)
  return listeners.flatMap(l => {
    const event = events.find(ev => ev.id === l.eventId)
    const publisherId = event?.publisherModuleId
    if (!publisherId || publisherId === l.toModuleId) return []
    return [{ eventId: l.eventId, eventName: event.name ?? l.eventId, from: publisherId, to: l.toModuleId }]
  })
}

// Offset control point so parallel edges between same pair don't overlap
function curvePath(x1, y1, x2, y2, offset = 0) {
  const mx = (x1 + x2) / 2
  const my = (y1 + y2) / 2
  const dx = x2 - x1, dy = y2 - y1
  const len = Math.sqrt(dx * dx + dy * dy) || 1
  const cx = mx - (dy / len) * offset
  const cy = my + (dx / len) * offset
  // Shorten line so arrowhead doesn't overlap the node circle
  const angle = Math.atan2(y2 - cy, x2 - cx)
  const tx = x2 - Math.cos(angle) * (NODE_R + 4)
  const ty = y2 - Math.sin(angle) * (NODE_R + 4)
  const sx = x1 + Math.cos(Math.atan2(cy - y1, cx - x1)) * (NODE_R + 4)
  const sy = y1 + Math.sin(Math.atan2(cy - y1, cx - x1)) * (NODE_R + 4)
  return { d: `M${sx},${sy} Q${cx},${cy} ${tx},${ty}`, labelX: cx, labelY: cy }
}

function DetailPanel({ module, flows, events, onClose }) {
  const outgoing = flows.filter(f => f.from === module.id)
  const incoming = flows.filter(f => f.to === module.id)

  return (
    <div style={{
      marginTop: '16px', background: '#1a1f2e', border: '1px solid #2d3748',
      borderRadius: '8px', padding: '18px 22px', position: 'relative',
    }}>
      <button onClick={onClose} style={{
        position: 'absolute', top: '12px', right: '14px', background: 'none',
        border: 'none', color: '#718096', cursor: 'pointer', fontSize: '18px', lineHeight: 1,
      }}>✕</button>

      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
        <div style={{ width: 10, height: 10, borderRadius: '50%', background: STATUS_COLOR[module.status] ?? '#4a5568' }} />
        <span style={{ fontWeight: '700', fontSize: '15px' }}>{module.name ?? module.id}</span>
        <span style={{ fontSize: '12px', color: STATUS_COLOR[module.status] ?? '#718096' }}>{module.status}</span>
        <span style={{ fontSize: '12px', color: '#4a5568', marginLeft: '8px' }}>{module.beanCount} beans</span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', fontSize: '13px' }}>
        <div>
          <div style={{ color: '#63b3ed', fontWeight: '600', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span>▶</span> Publishes
          </div>
          {outgoing.length === 0
            ? <div style={{ color: '#4a5568', fontStyle: 'italic' }}>no outgoing events</div>
            : outgoing.map((f, i) => (
              <div key={i} style={{ marginBottom: '6px', paddingLeft: '8px', borderLeft: '2px solid #63b3ed' }}>
                <div style={{ color: '#63b3ed', fontFamily: 'monospace', fontSize: '12px' }}>{f.eventName}</div>
                <div style={{ color: '#718096', fontSize: '11px', marginTop: '2px' }}>→ {f.to}</div>
              </div>
            ))
          }
        </div>
        <div>
          <div style={{ color: '#68d391', fontWeight: '600', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span>◀</span> Listens to
          </div>
          {incoming.length === 0
            ? <div style={{ color: '#4a5568', fontStyle: 'italic' }}>no incoming events</div>
            : incoming.map((f, i) => (
              <div key={i} style={{ marginBottom: '6px', paddingLeft: '8px', borderLeft: '2px solid #68d391' }}>
                <div style={{ color: '#68d391', fontFamily: 'monospace', fontSize: '12px' }}>{f.eventName}</div>
                <div style={{ color: '#718096', fontSize: '11px', marginTop: '2px' }}>← {f.from}</div>
              </div>
            ))
          }
        </div>
      </div>
    </div>
  )
}

export default function ModuleGraph({ modules, events, edges }) {
  const [selected, setSelected] = useState(null)
  const [hovered, setHovered] = useState(null)

  if (!modules || modules.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '60px', color: '#718096' }}>
        No modules found. Is the Eventus extractor running?
      </div>
    )
  }

  const positioned = layoutNodes(modules)
  const posById = Object.fromEntries(positioned.map(m => [m.id, m]))
  const flows = buildFlowEdges(events, edges)

  // Group flows by (from,to) pair to offset parallel edges
  const pairCount = {}
  flows.forEach(f => {
    const key = [f.from, f.to].sort().join('|')
    pairCount[key] = (pairCount[key] ?? 0) + 1
  })
  const pairIndex = {}
  const renderedFlows = flows.map(f => {
    const key = [f.from, f.to].sort().join('|')
    const idx = pairIndex[key] ?? 0
    pairIndex[key] = idx + 1
    const total = pairCount[key]
    const offset = (idx - (total - 1) / 2) * 28
    const from = posById[f.from], to = posById[f.to]
    if (!from || !to) return null
    return { ...f, from, to, ...curvePath(from.x, from.y, to.x, to.y, offset) }
  }).filter(Boolean)

  const focus = selected ?? hovered
  const selectedModule = modules.find(m => m.id === selected)

  return (
    <div>
      <div style={{ marginBottom: '12px', fontSize: '13px', color: '#718096', display: 'flex', alignItems: 'center', gap: '16px' }}>
        <span>{modules.length} module{modules.length !== 1 ? 's' : ''} · {events.length} event{events.length !== 1 ? 's' : ''} · {flows.length} flow{flows.length !== 1 ? 's' : ''}</span>
        <span style={{ color: '#4a5568' }}>click or hover a node to inspect</span>
        <span style={{ display: 'flex', gap: '12px', marginLeft: 'auto' }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <svg width="28" height="10"><line x1="0" y1="5" x2="22" y2="5" stroke="#63b3ed" strokeWidth="2" strokeDasharray="4 2"/><polygon points="22,2 28,5 22,8" fill="#63b3ed"/></svg>
            <span style={{ fontSize: '11px' }}>publishes</span>
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <svg width="28" height="10"><line x1="0" y1="5" x2="22" y2="5" stroke="#68d391" strokeWidth="2" strokeDasharray="4 2"/><polygon points="22,2 28,5 22,8" fill="#68d391"/></svg>
            <span style={{ fontSize: '11px' }}>listens</span>
          </span>
        </span>
      </div>

      <svg width={W} height={H} style={{ background: '#1a1f2e', borderRadius: '8px', border: '1px solid #2d3748' }}
        onClick={() => setSelected(null)}>
        <defs>
          <marker id="arr-pub" markerWidth="8" markerHeight="8" refX="7" refY="3" orient="auto">
            <path d="M0,0 L0,6 L8,3 z" fill="#63b3ed" />
          </marker>
          <marker id="arr-pub-hl" markerWidth="8" markerHeight="8" refX="7" refY="3" orient="auto">
            <path d="M0,0 L0,6 L8,3 z" fill="#90cdf4" />
          </marker>
          <marker id="arr-listen" markerWidth="8" markerHeight="8" refX="7" refY="3" orient="auto">
            <path d="M0,0 L0,6 L8,3 z" fill="#68d391" />
          </marker>
          <marker id="arr-listen-hl" markerWidth="8" markerHeight="8" refX="7" refY="3" orient="auto">
            <path d="M0,0 L0,6 L8,3 z" fill="#9ae6b4" />
          </marker>
        </defs>

        {renderedFlows.map((f, i) => {
          const isOut = focus && f.from === focus
          const isIn = focus && f.to === focus
          const isRelated = isOut || isIn
          const dimmed = focus && !isRelated
          const color = isOut ? '#90cdf4' : isIn ? '#9ae6b4' : f.from === hovered || f.to === hovered ? '#a0aec0' : '#4a5568'
          const marker = isOut ? 'url(#arr-pub-hl)' : isIn ? 'url(#arr-listen-hl)' : 'url(#arr-pub)'
          return (
            <g key={i} opacity={dimmed ? 0.12 : 1}>
              <path d={f.d} fill="none" stroke={color}
                strokeWidth={isRelated ? 2.5 : 1.5} strokeDasharray="5 3"
                markerEnd={marker} />
              <text x={f.labelX} y={f.labelY - 7} textAnchor="middle"
                fill={color} fontSize="10" fontFamily="monospace"
                style={{ pointerEvents: 'none' }}>
                {f.eventName}
              </text>
            </g>
          )
        })}

        {positioned.map(m => {
          const isSelected = m.id === selected
          const isHovered = m.id === hovered
          const isDimmed = focus && m.id !== focus
          const strokeColor = isSelected || isHovered ? '#90cdf4' : STATUS_COLOR[m.status] ?? '#4a5568'
          return (
            <g key={m.id} transform={`translate(${m.x},${m.y})`}
              style={{ cursor: 'pointer' }}
              opacity={isDimmed ? 0.25 : 1}
              onClick={e => { e.stopPropagation(); setSelected(isSelected ? null : m.id) }}
              onMouseEnter={() => setHovered(m.id)}
              onMouseLeave={() => setHovered(null)}>
              <circle r={NODE_R + 6} fill="transparent" />
              <circle r={NODE_R} fill={isSelected ? '#1e3a5f' : isHovered ? '#243048' : '#1e293b'}
                stroke={strokeColor} strokeWidth={isSelected ? 3 : isHovered ? 2.5 : 2} />
              <text textAnchor="middle" dominantBaseline="middle" fill="#e2e8f0"
                fontSize="11" fontWeight="600" style={{ pointerEvents: 'none' }}>{m.name ?? m.id}</text>
              <text textAnchor="middle" y={NODE_R + 16} fill={STATUS_COLOR[m.status] ?? '#718096'}
                fontSize="10" style={{ pointerEvents: 'none' }}>{m.status}</text>
            </g>
          )
        })}
      </svg>

      {selectedModule
        ? <DetailPanel module={selectedModule} flows={renderedFlows} events={events} onClose={() => setSelected(null)} />
        : (
          <div style={{ marginTop: '14px', display: 'flex', flexWrap: 'wrap', gap: '20px', fontSize: '12px', color: '#718096' }}>
            {modules.map(m => (
              <div key={m.id}
                style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer',
                  opacity: focus && m.id !== focus ? 0.4 : 1 }}
                onClick={() => setSelected(m.id)}
                onMouseEnter={() => setHovered(m.id)}
                onMouseLeave={() => setHovered(null)}>
                <div style={{ width: 8, height: 8, borderRadius: '50%', background: STATUS_COLOR[m.status] ?? '#4a5568' }} />
                <span>{m.name ?? m.id}</span>
                <span style={{ color: '#4a5568' }}>{m.beanCount} beans</span>
              </div>
            ))}
          </div>
        )
      }
    </div>
  )
}
