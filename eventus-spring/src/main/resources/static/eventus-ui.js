'use strict';

(function () {
    const { useState, useEffect, useCallback } = React;

    const STATUS_COLORS = {
        HEALTHY: '#10b981',
        WARNING: '#f59e0b',
        ERROR:   '#ef4444',
        default: '#6b7280',
    };

    const NODE_W = 160;
    const NODE_H = 72;
    const GAP_X  = 200;
    const GAP_Y  = 110;

    function computeLayout(modules, edges) {
        const publishers = new Set();
        const listeners  = new Set();
        edges.forEach(e => {
            if (e.edgeType === 'PUBLISHES'  && e.fromModuleId) publishers.add(e.fromModuleId);
            if (e.edgeType === 'LISTENS_TO' && e.toModuleId)   listeners.add(e.toModuleId);
        });

        const pubOnly  = modules.filter(m =>  publishers.has(m.id) && !listeners.has(m.id));
        const both     = modules.filter(m =>  publishers.has(m.id) &&  listeners.has(m.id));
        const lisOnly  = modules.filter(m => !publishers.has(m.id) &&  listeners.has(m.id));
        const neither  = modules.filter(m => !publishers.has(m.id) && !listeners.has(m.id));

        const columns = [pubOnly, both, lisOnly, neither].filter(c => c.length > 0);
        if (columns.length === 0) columns.push(modules);

        const positions = {};
        let maxRows = 0;

        columns.forEach((col, ci) => {
            maxRows = Math.max(maxRows, col.length);
            const cx = 60 + ci * (NODE_W + GAP_X);
            col.forEach((m, ri) => {
                positions[m.id] = { x: cx, y: 60 + ri * GAP_Y };
            });
        });

        const svgW = Math.max(700, columns.length * (NODE_W + GAP_X) + 60);
        const svgH = Math.max(380, maxRows * GAP_Y + 100);
        return { positions, svgW, svgH };
    }

    function buildLogicalEdges(edges, events) {
        const eventNames = {};
        events.forEach(ev => { eventNames[ev.id] = ev.name; });

        const publisherOf = {};
        edges.forEach(e => {
            if (e.edgeType === 'PUBLISHES' && e.fromModuleId) publisherOf[e.eventId] = e.fromModuleId;
        });

        const seen = new Set();
        const result = [];
        edges.forEach(e => {
            if (e.edgeType !== 'LISTENS_TO' || !e.toModuleId) return;
            const from = publisherOf[e.eventId];
            if (!from) return;
            const key = `${from}|${e.toModuleId}|${e.eventId}`;
            if (seen.has(key)) return;
            seen.add(key);
            result.push({ from, to: e.toModuleId, label: eventNames[e.eventId] || e.eventId });
        });
        return result;
    }

    function GraphEdge({ fromPos, toPos, label, offsetY }) {
        if (!fromPos || !toPos) return null;

        const x1 = fromPos.x + NODE_W;
        const y1 = fromPos.y + NODE_H / 2;
        const x2 = toPos.x;
        const y2 = toPos.y + NODE_H / 2;

        const mx = (x1 + x2) / 2;
        const my = (y1 + y2) / 2 + offsetY;
        const path = `M ${x1} ${y1} Q ${mx} ${my} ${x2} ${y2}`;

        const lx = 0.25 * x1 + 0.5 * mx + 0.25 * x2;
        const ly = 0.25 * y1 + 0.5 * my + 0.25 * y2 - 10;

        return React.createElement('g', null,
            React.createElement('path', { d: path, fill: 'none', stroke: '#9ca3af', strokeWidth: 2, markerEnd: 'url(#arrow)' }),
            React.createElement('text', { x: lx, y: ly, fontSize: 11, fill: '#4b5563', textAnchor: 'middle' }, label)
        );
    }

    function ModuleBox({ m, pos, selected, onClick }) {
        const color = STATUS_COLORS[m.status] || STATUS_COLORS.default;
        return React.createElement('g', {
            transform: `translate(${pos.x},${pos.y})`,
            onClick: () => onClick(m),
            style: { cursor: 'pointer' },
        },
            React.createElement('rect', {
                width: NODE_W, height: NODE_H, rx: 8,
                fill: color,
                stroke: selected ? '#1d4ed8' : 'rgba(0,0,0,0.1)',
                strokeWidth: selected ? 3 : 1,
                style: { filter: 'drop-shadow(0 2px 6px rgba(0,0,0,0.18))' },
            }),
            React.createElement('text', {
                x: NODE_W / 2, y: NODE_H / 2 - 8,
                fill: 'white', fontSize: 14, fontWeight: 700,
                textAnchor: 'middle', dominantBaseline: 'middle',
            }, m.name || m.id),
            React.createElement('text', {
                x: NODE_W / 2, y: NODE_H / 2 + 14,
                fill: 'rgba(255,255,255,0.82)', fontSize: 11, textAnchor: 'middle',
            }, m.beanCount + ' bean' + (m.beanCount !== 1 ? 's' : ''))
        );
    }

    function GraphCanvas({ modules, events, edges, selectedId, onSelect }) {
        const { positions, svgW, svgH } = computeLayout(modules, edges);
        const logicalEdges = buildLogicalEdges(edges, events);

        const pairCount = {};
        logicalEdges.forEach(e => { const k = `${e.from}|${e.to}`; pairCount[k] = (pairCount[k] || 0) + 1; });
        const pairSeen = {};

        const edgeEls = logicalEdges.map((e, i) => {
            const k = `${e.from}|${e.to}`;
            const idx = (pairSeen[k] = (pairSeen[k] || 0));
            pairSeen[k]++;
            const total = pairCount[k];
            const offsetY = (idx - (total - 1) / 2) * 22;
            return React.createElement(GraphEdge, {
                key: i,
                fromPos: positions[e.from],
                toPos: positions[e.to],
                label: e.label,
                offsetY,
            });
        });

        const nodeEls = modules
            .filter(m => positions[m.id])
            .map(m => React.createElement(ModuleBox, {
                key: m.id, m, pos: positions[m.id],
                selected: m.id === selectedId,
                onClick: onSelect,
            }));

        return React.createElement('div', { className: 'canvas-wrap' },
            React.createElement('svg', {
                width: '100%',
                viewBox: `0 0 ${svgW} ${svgH}`,
                style: { display: 'block', minHeight: svgH },
            },
                React.createElement('defs', null,
                    React.createElement('marker', {
                        id: 'arrow', markerWidth: 10, markerHeight: 7,
                        refX: 9, refY: 3.5, orient: 'auto',
                    },
                        React.createElement('polygon', { points: '0 0, 10 3.5, 0 7', fill: '#9ca3af' })
                    )
                ),
                React.createElement('rect', { width: svgW, height: svgH, fill: '#f9fafb' }),
                ...edgeEls,
                ...nodeEls
            )
        );
    }

    function DetailPanel({ module: m, events, edges, onClose }) {
        if (!m) return null;
        const published = events.filter(ev => ev.publisherModuleId === m.id);
        const listenedIds = new Set(
            edges.filter(e => e.edgeType === 'LISTENS_TO' && e.toModuleId === m.id).map(e => e.eventId)
        );
        const listened = events.filter(ev => listenedIds.has(ev.id));
        const color = STATUS_COLORS[m.status] || STATUS_COLORS.default;

        return React.createElement('div', { className: 'detail-panel' },
            React.createElement('div', { className: 'detail-panel-header' },
                React.createElement('h3', null, m.name),
                React.createElement('button', { className: 'close-btn', onClick: onClose }, '×')
            ),
            React.createElement('div', { className: 'detail-panel-body' },
                React.createElement('div', { className: 'detail-row' },
                    React.createElement('span', { className: 'detail-label' }, 'Status'),
                    React.createElement('span', { className: 'status-badge', style: { background: color } }, m.status)
                ),
                React.createElement('div', { className: 'detail-row' },
                    React.createElement('span', { className: 'detail-label' }, 'Beans'),
                    React.createElement('span', null, m.beanCount ?? '—')
                ),
                published.length > 0 && React.createElement('div', { className: 'detail-section' },
                    React.createElement('div', { className: 'detail-label' }, 'Publishes'),
                    ...published.map(ev => React.createElement('div', { key: ev.id, className: 'event-pill pub' }, ev.name))
                ),
                listened.length > 0 && React.createElement('div', { className: 'detail-section' },
                    React.createElement('div', { className: 'detail-label' }, 'Listens to'),
                    ...listened.map(ev => React.createElement('div', { key: ev.id, className: 'event-pill sub' }, ev.name))
                )
            )
        );
    }

    function Header({ refreshing, lastUpdated, moduleCount, eventCount }) {
        return React.createElement('div', { className: 'header' },
            React.createElement('div', { className: 'header-brand' },
                React.createElement('span', { className: 'header-title' }, 'Eventus'),
                React.createElement('span', { className: 'header-tagline' }, 'Event Topology')
            ),
            React.createElement('div', { className: 'header-stats' },
                React.createElement('span', { className: 'stat' }, moduleCount, ' modules'),
                React.createElement('span', { className: 'stat' }, eventCount, ' events'),
                refreshing
                    ? React.createElement('span', { className: 'refreshing' }, '↻ refreshing…')
                    : lastUpdated && React.createElement('span', { className: 'last-updated' }, 'Updated ', lastUpdated.toLocaleTimeString())
            )
        );
    }

    function EventusApp() {
        const [data, setData]               = useState({ modules: [], events: [], edges: [] });
        const [loading, setLoading]         = useState(true);
        const [refreshing, setRefreshing]   = useState(false);
        const [selected, setSelected]       = useState(null);
        const [lastUpdated, setLastUpdated] = useState(null);
        const [error, setError]             = useState(null);

        const fetchGraph = useCallback(async (initial) => {
            if (initial) setLoading(true); else setRefreshing(true);
            try {
                const res = await fetch('/eventus/api/graph');
                if (!res.ok) throw new Error('HTTP ' + res.status);
                const d = await res.json();
                setData({ modules: d.modules || [], events: d.events || [], edges: d.edges || [] });
                setLastUpdated(new Date());
                setError(null);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
                setRefreshing(false);
            }
        }, []);

        useEffect(() => {
            fetchGraph(true);
            const id = setInterval(() => fetchGraph(false), 5000);
            return () => clearInterval(id);
        }, [fetchGraph]);

        if (loading) return React.createElement('div', { className: 'splash' },
            React.createElement('div', { className: 'spinner' }),
            React.createElement('p', null, 'Loading event topology…')
        );

        if (error) return React.createElement('div', { className: 'splash error' },
            React.createElement('p', null, 'Could not load graph: ', error)
        );

        return React.createElement('div', { className: 'app' },
            React.createElement(Header, { refreshing, lastUpdated, moduleCount: data.modules.length, eventCount: data.events.length }),
            React.createElement('div', { className: 'canvas' },
                React.createElement(GraphCanvas, {
                    modules: data.modules, events: data.events, edges: data.edges,
                    selectedId: selected?.id, onSelect: setSelected,
                }),
                React.createElement(DetailPanel, {
                    module: selected, events: data.events, edges: data.edges,
                    onClose: () => setSelected(null),
                })
            )
        );
    }

    ReactDOM.createRoot(document.getElementById('root')).render(React.createElement(EventusApp));
}());
