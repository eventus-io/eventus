# S10 — Embedded Topology UI (v0.2)

## Goal
Ship the embedded React topology dashboard at `GET /eventus` — a pixel-faithful port of
the `docs/brand/topology-prototype/` prototype, rewritten in TypeScript, wired to live
actuator data, and built by Maven via `frontend-maven-plugin`.

## Stack
- Vite 5 + React 18 + TypeScript (strict: true, ES2022)
- vitest + React Testing Library for unit tests
- Maven-orchestrated via `frontend-maven-plugin` (Node 22.x)
- No external runtime CDN dependencies

## Surfaces (1:1 with topology-app.jsx prototype)
| Component    | Description                                                  |
|--------------|--------------------------------------------------------------|
| `TopBar`     | Lockup, breadcrumb, zoom chip, refresh button, live indicator |
| `Sidebar`    | Filter input, Modules / Events / Publications sections        |
| `Canvas`     | SVG graph: module rects, event circles, publish/subscribe edges |
| `Inspector`  | Right panel: module or event detail, RawJson viewer           |

## Live data
- Single fetch: `GET /eventus/api/graph` → `{ modules, events, edges, publications }`
- 2-second poll interval via `useTopology` hook
- AbortController cancels in-flight request on unmount / new poll
- On error: keep last good data, show error toast
- `useEdges` derives:
  - Per-event `consumers: string[]` from `LISTENS_TO` edges (`edge.toModuleId`)
  - Per-event `incomplete: boolean` from publications with `status !== 'COMPLETED'`
  - Canvas edge list: `publish` (module→event) + `subscribe` (event→module)

## Layout
- Deterministic two-column layout: modules at x=150, events at x=520, evenly spaced vertically
- ViewBox: `0 0 1000 600`
- `computeLayout(modules, richEvents)` in `src/layout.ts`

## Brand
- Geist + Geist Mono via Google Fonts (self-hosted woff2 in v0.3)
- CSS custom properties from `docs/brand/tokens.css` — copied into `src/tokens.css`

## Acceptance criteria
- [ ] `npm run build` produces assets in `../resources/static/eventus/`
- [ ] `npm test` (vitest run) — App, Canvas, Inspector tests green
- [ ] `mvn -pl eventus-ui clean install -DskipTests` builds JAR with bundled assets
- [ ] `GET /eventus` (or `/eventus/index.html`) returns 200 HTML in integration test
- [ ] Topology graph renders module rects + event circles + edges
- [ ] Click module → Inspector shows published + subscribed events
- [ ] Click event → Inspector shows publisher chip + consumer chips
- [ ] Esc key: clear selected → then clear filter
- [ ] Sidebar filter dims unmatched nodes on canvas
- [ ] Refresh button triggers pulse animation on live indicator

## API shape reference

```
GET /eventus/api/graph → {
  modules:      [{ id, name, beanCount, aggregateCount, status }]
  events:       [{ id (FQN), name (simple), publisherModuleId }]
  edges:        [{ id, eventId (FQN), fromModuleId, toModuleId, edgeType }]
  publications: [{ id, eventType, listenerName, moduleId, status, publishedAt }]
}

EdgeType:          PUBLISHES | LISTENS_TO | DEPENDS_ON
ModuleStatus:      HEALTHY | WARNING | ERROR
PublicationStatus: COMPLETED | INCOMPLETE | STALE
```

## Done when
- Spring integration test at `/eventus` returns HTTP 200
- All unit tests pass
- Two atomic conventional commits staged (no push)
