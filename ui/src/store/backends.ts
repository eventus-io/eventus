export interface Backend {
  id: string;
  name: string;
  url: string;     // empty = same-origin (uses Vite proxy / nginx proxy)
  color: string;   // hex
  enabled: boolean;
}

const KEY = 'eventus.backends';

export const SERVICE_PALETTE = [
  '#3b82f6', // blue
  '#10b981', // emerald
  '#f59e0b', // amber
  '#ec4899', // pink
  '#8b5cf6', // violet
  '#06b6d4', // cyan
];

const INITIAL: Backend[] = [{
  id: 'local',
  name: 'local',
  url: '',
  color: SERVICE_PALETTE[0],
  enabled: true,
}];

export function loadBackends(): Backend[] {
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Backend[];
      if (Array.isArray(parsed) && parsed.length > 0) return parsed;
    }
  } catch { /* ignore */ }
  return INITIAL;
}

export function saveBackends(backends: Backend[]): void {
  localStorage.setItem(KEY, JSON.stringify(backends));
}

export function nextColor(existing: Backend[]): string {
  const used = new Set(existing.map(b => b.color));
  return SERVICE_PALETTE.find(c => !used.has(c)) ?? SERVICE_PALETTE[existing.length % SERVICE_PALETTE.length];
}
