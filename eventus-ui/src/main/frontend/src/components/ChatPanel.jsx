import { useState, useRef, useEffect } from 'react'

const SUGGESTIONS = [
  'Which modules listen to OrderPlaced?',
  'Show incomplete publications',
  'What is the status of each module?',
]

const s = {
  container: { maxWidth: '720px', margin: '0 auto' },
  messages: { minHeight: '320px', maxHeight: '480px', overflowY: 'auto', marginBottom: '16px', display: 'flex', flexDirection: 'column', gap: '12px' },
  msg: (role) => ({
    padding: '10px 14px', borderRadius: '8px', fontSize: '13px', lineHeight: '1.6', maxWidth: '85%',
    alignSelf: role === 'user' ? 'flex-end' : 'flex-start',
    background: role === 'user' ? '#2b4a7a' : '#1e293b',
    color: role === 'user' ? '#bee3f8' : '#e2e8f0',
    border: '1px solid ' + (role === 'user' ? '#3182ce' : '#2d3748'),
  }),
  chips: { display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '12px' },
  chip: { padding: '4px 10px', borderRadius: '20px', background: '#1e293b', border: '1px solid #2d3748', color: '#a0aec0', fontSize: '12px', cursor: 'pointer' },
  inputRow: { display: 'flex', gap: '8px' },
  input: { flex: 1, background: '#1a1f2e', border: '1px solid #2d3748', borderRadius: '6px', color: '#e2e8f0', padding: '10px 12px', fontSize: '13px', outline: 'none' },
  btn: (disabled) => ({
    padding: '10px 18px', borderRadius: '6px', background: disabled ? '#2d3748' : '#3182ce',
    color: disabled ? '#4a5568' : '#fff', border: 'none', cursor: disabled ? 'not-allowed' : 'pointer',
    fontSize: '13px', fontWeight: '500',
  }),
  loading: { alignSelf: 'flex-start', color: '#718096', fontSize: '13px', fontStyle: 'italic' },
}

export default function ChatPanel() {
  const [messages, setMessages] = useState([
    { role: 'assistant', text: 'Ask me about your module graph. Try one of the suggestions below.' }
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef(null)

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages, loading])

  const send = async (question) => {
    const q = (question ?? input).trim()
    if (!q || loading) return
    setInput('')
    setMessages(prev => [...prev, { role: 'user', text: q }])
    setLoading(true)
    try {
      const res = await fetch('/eventus/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question: q }),
      })
      const data = await res.json()
      setMessages(prev => [...prev, { role: 'assistant', text: res.ok ? data.answer : (data.answer ?? `Error ${res.status}`) }])
    } catch {
      setMessages(prev => [...prev, { role: 'assistant', text: 'Could not reach the server.' }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={s.container}>
      <div style={s.messages}>
        {messages.map((m, i) => (
          <div key={i} style={s.msg(m.role)}>{m.text}</div>
        ))}
        {loading && <div style={s.loading}>Thinking…</div>}
        <div ref={bottomRef} />
      </div>
      <div style={s.chips}>
        {SUGGESTIONS.map(q => (
          <button key={q} style={s.chip} onClick={() => send(q)}>{q}</button>
        ))}
      </div>
      <div style={s.inputRow}>
        <input
          style={s.input}
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && send()}
          placeholder="Ask about your module graph…"
          disabled={loading}
        />
        <button style={s.btn(loading || !input.trim())} onClick={() => send()} disabled={loading || !input.trim()}>
          Send
        </button>
      </div>
    </div>
  )
}
