const STATUS_COLOR = { HEALTHY: '#48bb78', WARNING: '#ed8936', ERROR: '#fc8181', UNKNOWN: '#718096' }

const W = 900, H = 500, NODE_R = 36

function layoutNodes(modules) {
  const count = modules.length
  return modules.map((m, i) => {
    const angle = (2 * Math.PI * i) / Math.max(count, 1) - Math.PI / 2
    const rx = count === 1 ? 0 : (W / 2 - NODE_R - 40)
    const ry = count === 1 ? 0 : (H / 2 - NODE_R - 40)
    return { ...m, x: W / 2 + rx * Math.cos(angle), y: H / 2 + ry * Math.sin(angle) }
  })
}

export default function ModuleGraph({ modules, events, edges }) {
  if (!modules || modules.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '60px', color: '#718096' }}>
        No modules found. Is the Eventus extractor running?
      </div>
    )
  }

  const positioned = layoutNodes(modules)
  const posById = Object.fromEntries(positioned.map(m => [m.id, m]))

  const edgeLines = edges
    .filter(e => e.fromModuleId && e.toModuleId)
    .map(e => {
      const from = posById[e.fromModuleId]
      const to = posById[e.toModuleId]
      if (!from || !to) return null
      const event = events.find(ev => ev.id === e.eventId)
      return { ...e, from, to, eventName: event?.name ?? e.eventId }
    })
    .filter(Boolean)

  return (
    <div>
      <div style={{ marginBottom: '12px', fontSize: '13px', color: '#718096' }}>
        {modules.length} module{modules.length !== 1 ? 's' : ''} · {events.length} event{events.length !== 1 ? 's' : ''}
      </div>
      <svg width={W} height={H} style={{ background: '#1a1f2e', borderRadius: '8px', border: '1px solid #2d3748' }}>
        <defs>
          <marker id="arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
            <path d="M0,0 L0,6 L8,3 z" fill="#4a5568" />
          </marker>
          <marker id="arrow-publishes" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
            <path d="M0,0 L0,6 L8,3 z" fill="#63b3ed" />
          </marker>
        </defs>

        {edgeLines.map((e, i) => {
          const color = e.edgeType === 'PUBLISHES' ? '#63b3ed' : '#68d391'
          const mx = (e.from.x + e.to.x) / 2
          const my = (e.from.y + e.to.y) / 2
          return (
            <g key={i}>
              <line x1={e.from.x} y1={e.from.y} x2={e.to.x} y2={e.to.y}
                stroke={color} strokeWidth="1.5" strokeDasharray="4 2" opacity="0.6"
                markerEnd="url(#arrow)" />
              <text x={mx} y={my - 6} textAnchor="middle" fill={color}
                fontSize="10" fontFamily="monospace">{e.eventName}</text>
            </g>
          )
        })}

        {positioned.map(m => (
          <g key={m.id} transform={`translate(${m.x},${m.y})`}>
            <circle r={NODE_R} fill="#1e293b" stroke={STATUS_COLOR[m.status] ?? '#4a5568'} strokeWidth="2" />
            <text textAnchor="middle" dominantBaseline="middle" fill="#e2e8f0"
              fontSize="11" fontWeight="600">{m.name ?? m.id}</text>
            <text textAnchor="middle" y={NODE_R + 14} fill={STATUS_COLOR[m.status] ?? '#718096'}
              fontSize="10">{m.status}</text>
          </g>
        ))}
      </svg>

      <div style={{ marginTop: '16px', display: 'flex', gap: '24px', fontSize: '12px', color: '#718096' }}>
        {modules.map(m => (
          <div key={m.id} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <div style={{ width: 8, height: 8, borderRadius: '50%', background: STATUS_COLOR[m.status] ?? '#4a5568' }} />
            <span>{m.name ?? m.id}</span>
            <span style={{ color: '#4a5568' }}>{m.beanCount} beans</span>
          </div>
        ))}
      </div>
    </div>
  )
}
