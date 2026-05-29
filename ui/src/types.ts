// Shapes returned by GET /eventus/api/graph

export type ModuleStatus = 'HEALTHY' | 'WARNING' | 'ERROR';
export type EdgeType = 'PUBLISHES' | 'LISTENS_TO' | 'DEPENDS_ON';
export type PublicationStatus = 'COMPLETED' | 'INCOMPLETE' | 'STALE';
export type ViolationSeverity = 'ERROR' | 'WARNING' | 'INFO';
export type DriftSeverity = 'BREAKING' | 'MODERATE' | 'MINOR';
export type ActiveView = 'graph' | 'impact' | 'violations' | 'drift' | 'publications';

export interface Module {
  id: string;
  name: string;
  beanCount: number;
  aggregateCount: number;
  status: ModuleStatus;
}

export interface EventDef {
  id: string;            // FQN (e.g. "com.example.order.OrderPlaced")
  name: string;          // simple name (e.g. "OrderPlaced")
  publisherModuleId: string;
}

export interface EdgeDef {
  id: string;
  eventId: string;
  fromModuleId: string | null;
  toModuleId: string | null;
  edgeType: EdgeType;
}

export interface Publication {
  id: string;
  eventType: string;
  listenerName: string;
  moduleId: string;
  status: PublicationStatus;
  publishedAt: string;
}

export interface GraphData {
  modules: Module[];
  events: EventDef[];
  edges: EdgeDef[];
  publications: Publication[];
}

// Derived event with consumers + incomplete flag (prototype-compatible)
export interface RichEvent {
  id: string;         // FQN — used as stable key
  simpleName: string; // display label
  publisher: string;  // module id
  consumers: string[];
  incomplete: boolean;
}

// Edge for SVG rendering
export interface CanvasEdge {
  kind: 'publish' | 'subscribe';
  from: { type: 'module' | 'event'; id: string };
  to: { type: 'module' | 'event'; id: string };
  eventId: string;
}

export interface Layout {
  modules: Record<string, { x: number; y: number }>;
  events: Record<string, { x: number; y: number }>;
}

export interface Selection {
  type: 'module' | 'event';
  id: string;
}

// ─── Impact Analysis ───────────────────────────────────────

export interface AffectedModule {
  moduleId: string;
  moduleName: string;
  relationshipType: string;
  isDirectListener: boolean;
}

export interface EventImpactResult {
  eventId: string;
  eventName: string;
  publisherModuleId: string;
  directListeners: number;
  indirectConsumers: number;
  affectedModules: AffectedModule[];
}

export interface EventInfo {
  eventId: string;
  eventName: string;
  directListeners: number;
  listenerModuleIds: string[];
}

export interface DownstreamModule {
  moduleId: string;
  moduleName: string;
  relationshipType: string;
}

export interface ModuleImpactResult {
  moduleId: string;
  moduleName: string;
  publishedEvents: EventInfo[];
  downstreamModules: DownstreamModule[];
  totalAffectedModules: number;
}

// ─── Violations ────────────────────────────────────────────

export interface ViolationItem {
  id: string;
  type: string;
  severity: ViolationSeverity;
  title: string;
  description: string;
  affectedModuleIds: string[];
  affectedEventIds: string[];
  detectedAt: number;
}

// ─── Drift ─────────────────────────────────────────────────

export interface DriftItem {
  id: string;
  type: string;
  severity: DriftSeverity;
  title: string;
  description: string;
  affectedItemId: string;
  affectedItemName: string;
  detectedAt: number;
}

export interface DriftReport {
  drifts: DriftItem[];
  totalDrifts: number;
  breachingCount: number;
  comparedAt: number;
}
