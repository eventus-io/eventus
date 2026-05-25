# Eventus — brand & design system

> Event topology, made visible.

This is the visual identity for **Eventus**, an open-source JVM library that
makes the event and module topology of your application visible. The system is
dark-first, mono-forward, and built around a single graph-topology motif.

---

## 1 · Voice

| Use this | Not this |
|---|---|
| **Event topology, made visible.** | Visualize your microservices |
| Drop it in — start at the topology. | Get started in 5 minutes! |
| Modules. Events. Publications. | Insights. Observability. Real-time. |

Voice is **precise, technical, and quiet**. Short sentences. No marketing words
(*"powerful", "intelligent", "modern"*) and no emoji in product surfaces.
Numbers are concrete (`4 beans · 1 aggregate`, not "several"). The audience is
a backend engineer who already knows what Spring Modulith is.

---

## 2 · Mark

The mark is a publisher node radiating two satellite events — a stylized
lowercase `e` and a literal slice of an event graph.

| File | Use |
|---|---|
| `eventus-mark.svg` | Color mark on dark surfaces (#0a0a0a / #111) |
| `eventus-mark-on-light.svg` | Color mark on light surfaces |
| `eventus-mark-mono-light.svg` | Single-fill, light, for dark backgrounds |
| `eventus-mark-mono-dark.svg` | Single-fill, dark, for light backgrounds |
| `eventus-lockup-on-dark.svg` | Wordmark + mark, dark surface |
| `eventus-lockup-on-light.svg` | Wordmark + mark, light surface |
| `favicon.svg` | Optimized for 16/32px favicon use |

**Wordmark.** `eventus` in Geist Mono Medium (500), tracking -0.04em, lowercase.
Never set in Geist Sans or any other family.

**Lockup geometry.** Mark on the left, wordmark on the right. Gap between mark
and wordmark = the mark's stroke width × 4. Mark height matches wordmark cap
height × 1.1.

**Clear space.** Always reserve at least one mark height of clear space on
every side.

**Minimum size.** 16 px for the mark, 56 px for the lockup. Below 16 px use the
favicon variant.

**Don't.** Rotate the mark. Recolor it outside the violet ramp. Reflow the
lockup vertically. Use the wordmark without the mark on first-impression
surfaces (hero, social card, app shell).

---

## 3 · Color

Tokens live in `tokens.css`. Override on `:root` (dark, default) or on
`.eventus-light` for the inverted surface.

### Brand · violet ramp

The violet ramp is the only chromatic identity color. Same OKLCH hue (300°),
varied lightness + chroma across the ramp.

| Token | Value | Use |
|---|---|---|
| `--eventus-accent` | `oklch(72% 0.18 300)` ≈ `#9b7cf2` | Primary on dark |
| `--eventus-violet-500` | `oklch(70% 0.20 300)` | Canonical brand violet |
| Ramp 100 → 900 | see `tokens.css` | Surfaces, backgrounds, badges |

On light surfaces (`.eventus-light`), the accent shifts to `oklch(56% 0.22 300)`
≈ `#6a3ed4` for the contrast needed against `#fafaf7`.

### Surfaces · dark (default)

```
--eventus-bg            #0a0a0a   page
--eventus-bg-subtle     #111111   sidebar, header
--eventus-bg-raised     #171717   cards, inputs
--eventus-bg-inset      #080808   code wells
--eventus-border        #262626   1px hairline
--eventus-border-strong #3a3a3a   button outlines
```

### Text · dark

```
--eventus-fg        #ededed   body
--eventus-fg-mute   #a3a3a3   secondary
--eventus-fg-dim    #666666   labels
--eventus-fg-faint  #3f3f3f   disabled
```

### Semantic

| Token | Used for |
|---|---|
| `--eventus-success` | `HEALTHY` modules, publishes edges (alt) |
| `--eventus-warn` | `STALE` publications, slow refresh |
| `--eventus-danger` | `INCOMPLETE` publications, missing publisher |
| `--eventus-info` | `SUBSCRIBES` edges in the graph |

---

## 4 · Typography

Two families, both from Google Fonts.

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Geist:wght@300;400;500;600;700&family=Geist+Mono:wght@400;500;600;700&display=swap" rel="stylesheet">
```

| Family | Use |
|---|---|
| **Geist** sans | UI body, headings, buttons |
| **Geist Mono** | Code, identifiers, eyebrows, labels, metadata, the wordmark |

Eventus uses Geist Mono *more than most products do*. Module names, event
names, durations, IDs, and uppercase eyebrow labels are all mono. This is
deliberate — it signals that the product reads from real type-level data, not
made-up copy.

### Scale

| Token | Size | Weight | Tracking |
|---|---|---|---|
| Display | 72 | 600 | -0.025em |
| H1 | 48 | 600 | -0.022em |
| H2 | 32 | 600 | -0.02em |
| H3 | 22 | 600 | -0.015em |
| Body L | 17 | 400 | -0.005em |
| Body | 15 | 400 | 0 |
| Small | 13 | 400 | 0 |
| Caption | 11 | 500 | 0.14em uppercase (eyebrow) |

---

## 5 · Shape, spacing, motion

- **Radius:** 4 / 6 / 8 / 12 (sm / md / lg / xl)
- **Spacing:** 4px base — 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48
- **Hairlines:** always 1px, never thicker. Borders use `--eventus-border` on
  dark, `--eventus-border-strong` only for button outlines.
- **Motion:** 120ms for hover, 180ms for layout reflows. Always
  `cubic-bezier(0.2, 0.7, 0.3, 1)`. No bouncing.

---

## 6 · Iconography

20×20 grid, 1.5px stroke, round caps, round joins. Stroke uses
`currentColor` — colored from text/accent tokens, never hard-coded.

16 ships in v0.1:

`module · event · publication · aggregate · topology · healthy · stale · incomplete · subscribe · publish · graph · search · filter · settings · external · copy`

---

## 7 · Graph viz

The topology view is the most important surface. Tokens:

| Element | Treatment |
|---|---|
| Module node | Rounded rect, 36px tall, `--eventus-bg-raised` fill, 1.5px stroke from the health palette |
| Event node | r=6 filled circle, `--eventus-accent` |
| Publishes edge | 1.5px solid, `--eventus-accent`, arrow head |
| Subscribes edge | 1.5px dashed (4/4), `--eventus-info`, arrow head |
| Selected | Stroke `--eventus-accent`, never an external focus ring |

Background: a 24px dot grid at 60% opacity using `--eventus-border` as the dot
color. The grid is mandatory — it grounds the graph and signals "this is a
diagrammatic surface, not a chart."

---

## 8 · README & social

| Asset | Spec |
|---|---|
| OG card | 1200 × 630 · `og-card.svg` (source) + `og-card.html` (live render with Geist Mono loaded) — to mint the PNG, open `og-card.html` in a browser and screenshot at exactly 1200×630, or use Puppeteer/Playwright |
| Favicon | `favicon.svg` — also serve `favicon-32.png` and `apple-touch-icon.png` (180×180) |
| Shields | Use brand violet `#9b7cf2` as the right-side color for the canonical Eventus badge |

See `README-badges.md` for copy-pasteable badge URLs.

---

## 9 · File map

```
brand/
├── BRAND.md                       this file
├── README-badges.md               badge URLs
├── tokens.css                     CSS variables (dark + .eventus-light)
├── eventus-mark.svg               primary color mark (dark surfaces)
├── eventus-mark-on-light.svg      primary color mark (light surfaces)
├── eventus-mark-mono-light.svg    monochrome, light fill
├── eventus-mark-mono-dark.svg     monochrome, dark fill
├── eventus-lockup-on-dark.svg     wordmark + mark for dark
├── eventus-lockup-on-light.svg    wordmark + mark for light
├── favicon.svg                    favicon, rounded square
├── og-card.svg                    1200×630 social card (source)
└── brand-preview.html             single-page review of the whole system
```

When you land v0.2 (embedded UI), drop `tokens.css` into
`eventus-ui/src/main/resources/static/css/` and source it from the React shell.
When you land v0.3 (Grafana dashboard), the same tokens carry over — the dark
surface and violet accent map directly onto Grafana's panel theme.

---

*Visual identity v0.1 · locked direction: A · Topology.*
