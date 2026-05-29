import type {
  GraphData, EventImpactResult, ModuleImpactResult,
  ViolationItem, DriftReport,
} from './types';

async function get<T>(url: string, signal?: AbortSignal): Promise<T> {
  const res = await fetch(url, { signal });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json() as Promise<T>;
}

function base(url = ''): string {
  return url.replace(/\/$/, '');
}

export function fetchGraph(signal?: AbortSignal, baseUrl = ''): Promise<GraphData> {
  return get<GraphData>(`${base(baseUrl)}/eventus/api/graph`, signal);
}

export function fetchViolations(baseUrl = ''): Promise<ViolationItem[]> {
  return get<ViolationItem[]>(`${base(baseUrl)}/eventus/api/violations`);
}

export function fetchDrift(baseUrl = ''): Promise<DriftReport> {
  return get<DriftReport>(`${base(baseUrl)}/eventus/api/drift`);
}

export async function postBaseline(baseUrl = ''): Promise<void> {
  const res = await fetch(`${base(baseUrl)}/eventus/api/drift/baseline`, { method: 'POST' });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
}

export function fetchEventImpact(eventId: string, baseUrl = ''): Promise<EventImpactResult> {
  return get<EventImpactResult>(`${base(baseUrl)}/eventus/api/impact/event/${encodeURIComponent(eventId)}`);
}

export function fetchModuleImpact(moduleId: string, baseUrl = ''): Promise<ModuleImpactResult> {
  return get<ModuleImpactResult>(`${base(baseUrl)}/eventus/api/impact/module/${encodeURIComponent(moduleId)}`);
}
