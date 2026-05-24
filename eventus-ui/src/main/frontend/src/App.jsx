import { useState, useEffect } from 'react'
import ModuleGraph from './components/ModuleGraph.jsx'
import PublicationLog from './components/PublicationLog.jsx'
import ChatPanel from './components/ChatPanel.jsx'

const TABS = ['Graph', 'Publications', 'AI Query']

const styles = {
  app: { minHeight: '100vh', background: '#0f1117', color: '#e2e8f0' },
  header: { background: '#1a1f2e', borderBottom: '1px solid #2d3748', padding: '12px 24px', display: 'flex', alignItems: 'center', gap: '16px' },
  logo: { fontSize: '18px', fontWeight: '700', color: '#63b3ed', letterSpacing: '-0.5px' },
  tagline: { fontSize: '12px', color: '#718096' },
  tabs: { display: 'flex', gap: '4px', borderBottom: '1px solid #2d3748', padding: '0 24px', background: '#1a1f2e' },
  tab: (active) => ({
    padding: '10px 16px', cursor: 'pointer', fontSize: '13px', fontWeight: '500',
    color: active ? '#63b3ed' : '#718096',
    borderBottom: active ? '2px solid #63b3ed' : '2px solid transparent',
    background: 'none', border: 'none', borderRadius: '0',
  }),
  content: { padding: '24px' },
}

export default function App() {
  const [activeTab, setActiveTab] = useState(0)
  const [graph, setGraph] = useState({ modules: [], events: [], edges: [], publications: [] })
  const [error, setError] = useState(null)

  useEffect(() => {
    fetch('/eventus/api/graph')
      .then(r => r.json())
      .then(setGraph)
      .catch(() => setError('Could not load graph data from /eventus/api/graph'))
  }, [])

  return (
    <div style={styles.app}>
      <header style={styles.header}>
        <div>
          <div style={styles.logo}>Eventus</div>
          <div style={styles.tagline}>Event topology, made visible.</div>
        </div>
        {error && <span style={{ fontSize: '12px', color: '#fc8181', marginLeft: 'auto' }}>{error}</span>}
      </header>
      <nav style={styles.tabs}>
        {TABS.map((t, i) => (
          <button key={t} style={styles.tab(activeTab === i)} onClick={() => setActiveTab(i)}>{t}</button>
        ))}
      </nav>
      <main style={styles.content}>
        {activeTab === 0 && <ModuleGraph modules={graph.modules} events={graph.events} edges={graph.edges} />}
        {activeTab === 1 && <PublicationLog publications={graph.publications} />}
        {activeTab === 2 && <ChatPanel />}
      </main>
    </div>
  )
}
