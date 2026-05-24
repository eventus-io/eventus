const STATUS_BADGE = {
  COMPLETE: { bg: '#1c4532', color: '#68d391' },
  INCOMPLETE: { bg: '#7b341e', color: '#fc8181' },
  STALE: { bg: '#5c2626', color: '#feb2b2' },
}

function Badge({ status }) {
  const s = STATUS_BADGE[status] ?? { bg: '#2d3748', color: '#a0aec0' }
  return (
    <span style={{ background: s.bg, color: s.color, padding: '2px 8px', borderRadius: '4px', fontSize: '11px', fontWeight: '600' }}>
      {status}
    </span>
  )
}

function ago(iso) {
  if (!iso) return '—'
  const ms = Date.now() - new Date(iso).getTime()
  if (ms < 60000) return `${Math.floor(ms / 1000)}s ago`
  if (ms < 3600000) return `${Math.floor(ms / 60000)}m ago`
  return `${Math.floor(ms / 3600000)}h ago`
}

export default function PublicationLog({ publications }) {
  if (!publications || publications.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '60px', color: '#718096' }}>
        No incomplete publications. All events delivered successfully.
      </div>
    )
  }

  return (
    <div>
      <div style={{ marginBottom: '12px', fontSize: '13px', color: '#718096' }}>
        {publications.length} incomplete publication{publications.length !== 1 ? 's' : ''}
      </div>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
        <thead>
          <tr style={{ borderBottom: '1px solid #2d3748', color: '#718096', textAlign: 'left' }}>
            <th style={{ padding: '8px 12px' }}>Event</th>
            <th style={{ padding: '8px 12px' }}>Listener</th>
            <th style={{ padding: '8px 12px' }}>Module</th>
            <th style={{ padding: '8px 12px' }}>Status</th>
            <th style={{ padding: '8px 12px' }}>Age</th>
          </tr>
        </thead>
        <tbody>
          {publications.map((p, i) => (
            <tr key={p.id ?? i} style={{ borderBottom: '1px solid #1a1f2e' }}>
              <td style={{ padding: '10px 12px', fontFamily: 'monospace', color: '#63b3ed' }}>{p.eventType}</td>
              <td style={{ padding: '10px 12px', fontFamily: 'monospace', fontSize: '12px' }}>{p.listenerName}</td>
              <td style={{ padding: '10px 12px', color: '#a0aec0' }}>{p.moduleId}</td>
              <td style={{ padding: '10px 12px' }}><Badge status={p.status} /></td>
              <td style={{ padding: '10px 12px', color: '#718096' }}>{ago(p.publishedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
