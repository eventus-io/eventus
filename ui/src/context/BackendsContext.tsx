import { createContext, useContext, useState, useCallback } from 'react';
import type { Backend } from '../store/backends';
import { loadBackends, saveBackends, nextColor } from '../store/backends';

interface BackendsContextValue {
  backends: Backend[];
  addBackend: (name: string, url: string) => void;
  removeBackend: (id: string) => void;
  toggleBackend: (id: string) => void;
  updateBackend: (id: string, patch: Partial<Pick<Backend, 'name' | 'url'>>) => void;
}

const BackendsContext = createContext<BackendsContextValue | null>(null);

export function BackendsProvider({ children }: { children: React.ReactNode }) {
  const [backends, setBackends] = useState<Backend[]>(loadBackends);

  const addBackend = useCallback((name: string, url: string) => {
    setBackends(prev => {
      const next: Backend[] = [...prev, {
        id: crypto.randomUUID(),
        name: name.trim() || 'service',
        url: url.trim(),
        color: nextColor(prev),
        enabled: true,
      }];
      saveBackends(next);
      return next;
    });
  }, []);

  const removeBackend = useCallback((id: string) => {
    setBackends(prev => {
      const next = prev.filter(b => b.id !== id);
      const result = next.length > 0 ? next : loadBackends();
      saveBackends(result);
      return result;
    });
  }, []);

  const toggleBackend = useCallback((id: string) => {
    setBackends(prev => {
      const next = prev.map(b => b.id === id ? { ...b, enabled: !b.enabled } : b);
      saveBackends(next);
      return next;
    });
  }, []);

  const updateBackend = useCallback((id: string, patch: Partial<Pick<Backend, 'name' | 'url'>>) => {
    setBackends(prev => {
      const next = prev.map(b => b.id === id ? { ...b, ...patch } : b);
      saveBackends(next);
      return next;
    });
  }, []);

  return (
    <BackendsContext.Provider value={{ backends, addBackend, removeBackend, toggleBackend, updateBackend }}>
      {children}
    </BackendsContext.Provider>
  );
}

export function useBackends(): BackendsContextValue {
  const ctx = useContext(BackendsContext);
  if (!ctx) throw new Error('useBackends must be used within BackendsProvider');
  return ctx;
}
