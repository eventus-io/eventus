import { useState } from 'react';
import type { Backend } from '../store/backends';
import { useBackends } from '../context/BackendsContext';

interface BackendsPanelProps {
  errors: Map<string, string>;
  onClose: () => void;
}

export function BackendsPanel({ errors, onClose }: BackendsPanelProps) {
  const { backends, addBackend, removeBackend, toggleBackend, updateBackend } = useBackends();
  const [newName, setNewName] = useState('');
  const [newUrl, setNewUrl]   = useState('');

  function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    if (!newName.trim()) return;
    addBackend(newName, newUrl);
    setNewName('');
    setNewUrl('');
  }

  return (
    <div
      style={{
        position: 'fixed', inset: 0, zIndex: 100,
        background: 'rgba(0,0,0,0.55)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}
      onClick={e => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div style={{
        width: 520, maxWidth: '94vw',
        background: 'var(--eventus-bg-raised)',
        border: '1px solid var(--eventus-border)',
        borderRadius: 12,
        boxShadow: '0 20px 60px rgba(0,0,0,0.5)',
        overflow: 'hidden',
      }}>
        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '16px 20px',
          borderBottom: '1px solid var(--eventus-border)',
        }}>
          <div>
            <div style={{ fontFamily: 'var(--eventus-font-mono)', fontSize: 14, color: 'var(--eventus-fg)' }}>
              Backends
            </div>
            <div style={{ fontFamily: 'var(--eventus-font-sans)', fontSize: 12, color: 'var(--eventus-fg-dim)', marginTop: 2 }}>
              Manage connected Eventus services — graphs are merged into one canvas.
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              width: 28, height: 28,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              background: 'transparent', border: '1px solid var(--eventus-border)',
              borderRadius: 6, color: 'var(--eventus-fg-mute)',
              fontFamily: 'var(--eventus-font-mono)', fontSize: 16,
              cursor: 'pointer',
            }}
          >×</button>
        </div>

        {/* Backend list */}
        <div style={{ maxHeight: 320, overflowY: 'auto' }}>
          {backends.map(b => (
            <BackendRow
              key={b.id}
              backend={b}
              error={errors.get(b.id)}
              onToggle={() => toggleBackend(b.id)}
              onRemove={() => removeBackend(b.id)}
              onUpdate={(patch) => updateBackend(b.id, patch)}
              canRemove={backends.length > 1}
            />
          ))}
        </div>

        {/* Add form */}
        <form
          onSubmit={handleAdd}
          style={{
            display: 'flex', gap: 8, padding: '14px 20px',
            borderTop: '1px solid var(--eventus-border)',
            background: 'var(--eventus-bg-subtle)',
          }}
        >
          <input
            placeholder="name"
            value={newName}
            onChange={e => setNewName(e.target.value)}
            style={inputStyle}
          />
          <input
            placeholder="http://localhost:8080  (empty = same-origin)"
            value={newUrl}
            onChange={e => setNewUrl(e.target.value)}
            style={{ ...inputStyle, flex: 2 }}
          />
          <button
            type="submit"
            disabled={!newName.trim()}
            style={{
              height: 32, padding: '0 14px', borderRadius: 6,
              background: 'var(--eventus-accent)',
              border: 0,
              color: 'var(--eventus-accent-ink)',
              fontFamily: 'var(--eventus-font-mono)', fontSize: 12,
              cursor: newName.trim() ? 'pointer' : 'default',
              opacity: newName.trim() ? 1 : 0.5,
              flexShrink: 0,
            }}
          >Add</button>
        </form>
      </div>
    </div>
  );
}

interface BackendRowProps {
  backend: Backend;
  error?: string;
  onToggle: () => void;
  onRemove: () => void;
  onUpdate: (patch: Partial<Pick<Backend, 'name' | 'url'>>) => void;
  canRemove: boolean;
}

function BackendRow({ backend, error, onToggle, onRemove, onUpdate, canRemove }: BackendRowProps) {
  const [editName, setEditName] = useState(false);
  const [editUrl, setEditUrl]   = useState(false);
  const [name, setName] = useState(backend.name);
  const [url, setUrl]   = useState(backend.url);

  function commitName() {
    setEditName(false);
    if (name.trim() && name.trim() !== backend.name) onUpdate({ name: name.trim() });
    else setName(backend.name);
  }

  function commitUrl() {
    setEditUrl(false);
    if (url !== backend.url) onUpdate({ url: url.trim() });
  }

  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 10,
      padding: '10px 20px',
      borderBottom: '1px solid var(--eventus-border)',
      opacity: backend.enabled ? 1 : 0.55,
    }}>
      {/* Service color dot */}
      <span style={{
        width: 10, height: 10, borderRadius: 999,
        background: backend.color, flexShrink: 0,
      }} />

      {/* Name */}
      {editName ? (
        <input
          autoFocus
          value={name}
          onChange={e => setName(e.target.value)}
          onBlur={commitName}
          onKeyDown={e => { if (e.key === 'Enter') commitName(); if (e.key === 'Escape') { setEditName(false); setName(backend.name); } }}
          style={{ ...inputStyle, width: 100, flexShrink: 0 }}
        />
      ) : (
        <span
          title="Click to edit name"
          onClick={() => setEditName(true)}
          style={{
            fontFamily: 'var(--eventus-font-mono)', fontSize: 12,
            color: 'var(--eventus-fg)', cursor: 'text',
            width: 100, flexShrink: 0,
            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          }}
        >{backend.name}</span>
      )}

      {/* URL */}
      {editUrl ? (
        <input
          autoFocus
          value={url}
          onChange={e => setUrl(e.target.value)}
          onBlur={commitUrl}
          onKeyDown={e => { if (e.key === 'Enter') commitUrl(); if (e.key === 'Escape') { setEditUrl(false); setUrl(backend.url); } }}
          style={{ ...inputStyle, flex: 1, minWidth: 0 }}
        />
      ) : (
        <span
          title="Click to edit URL"
          onClick={() => setEditUrl(true)}
          style={{
            fontFamily: 'var(--eventus-font-mono)', fontSize: 11,
            color: backend.url ? 'var(--eventus-fg-mute)' : 'var(--eventus-fg-dim)',
            cursor: 'text', flex: 1, minWidth: 0,
            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          }}
        >{backend.url || '(same-origin)'}</span>
      )}

      {/* Error indicator */}
      {error && (
        <span title={error} style={{
          fontFamily: 'var(--eventus-font-mono)', fontSize: 10,
          color: 'var(--eventus-danger)', flexShrink: 0,
        }}>⚠ unreachable</span>
      )}

      {/* Toggle */}
      <Toggle on={backend.enabled} onClick={onToggle} />

      {/* Delete */}
      {canRemove && (
        <button
          onClick={onRemove}
          title="Remove backend"
          style={{
            width: 22, height: 22, flexShrink: 0,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            background: 'transparent',
            border: '1px solid transparent',
            borderRadius: 4, color: 'var(--eventus-fg-dim)',
            fontFamily: 'var(--eventus-font-mono)', fontSize: 14,
            cursor: 'pointer',
          }}
          onMouseEnter={e => { (e.currentTarget as HTMLElement).style.color = 'var(--eventus-danger)'; (e.currentTarget as HTMLElement).style.borderColor = 'color-mix(in oklch, var(--eventus-danger) 30%, transparent)'; }}
          onMouseLeave={e => { (e.currentTarget as HTMLElement).style.color = 'var(--eventus-fg-dim)'; (e.currentTarget as HTMLElement).style.borderColor = 'transparent'; }}
        >×</button>
      )}
    </div>
  );
}

function Toggle({ on, onClick }: { on: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      title={on ? 'Disable backend' : 'Enable backend'}
      style={{
        width: 32, height: 18, flexShrink: 0,
        borderRadius: 999, border: 0, cursor: 'pointer',
        background: on ? 'var(--eventus-accent)' : 'var(--eventus-bg-inset)',
        position: 'relative', transition: 'background .2s',
        padding: 0,
      }}
    >
      <span style={{
        position: 'absolute', top: 2, left: on ? 14 : 2,
        width: 14, height: 14, borderRadius: 999,
        background: on ? 'var(--eventus-accent-ink)' : 'var(--eventus-fg-dim)',
        transition: 'left .2s',
      }} />
    </button>
  );
}

const inputStyle: React.CSSProperties = {
  height: 32, flex: 1, minWidth: 0,
  background: 'var(--eventus-bg-inset)',
  border: '1px solid var(--eventus-border)',
  borderRadius: 6,
  color: 'var(--eventus-fg)',
  fontFamily: 'var(--eventus-font-mono)', fontSize: 12,
  padding: '0 10px',
  outline: 'none',
};
