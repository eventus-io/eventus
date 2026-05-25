import type {
  GraphData, EventImpactResult, ModuleImpactResult,
  ViolationItem, DriftReport,
} from './types';

async function get<T>(url: string, signal?: AbortSignal): Promise<T> {
  const res = await fetch(url, { signal });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json() as Promise<T>;
}

export function fetchGraph(signal?: AbortSignal): Promise<GraphData> {
  return get<GraphData>('/eventus/api/graph', signal);
}

export function fetchViolations(): Promise<ViolationItem[]> {
  return get<ViolationItem[]>('/eventus/api/violations');
}

export function fetchDrift(): Promise<DriftReport> {
  return get<DriftReport>('/eventus/api/drift');
}

export async function postBaseline(): Promise<void> {
  const res = await fetch('/eventus/api/drift/baseline', { method: 'POST' });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
}

export function fetchEventImpact(eventId: string): Promise<EventImpactResult> {
  return get<EventImpactResult>(`/eventus/api/impact/event/${encodeURIComponent(eventId)}`);
}

export function fetchModuleImpact(moduleId: string): Promise<ModuleImpactResult> {
  return get<ModuleImpactResult>(`/eventus/api/impact/module/${encodeURIComponent(moduleId)}`);
}
