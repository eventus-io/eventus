'use strict';

(function () {
    const { useState, useEffect, useCallback } = React;
    const ReactFlow = window.ReactFlow || {};
    const {
        ReactFlowProvider,
        ReactFlow: Flow,
        Background,
        Controls,
        MiniMap,
        Handle,
        Position,
        useNodesState,
        useEdgesState,
    } = ReactFlow;

    const STATUS_COLORS = {
        HEALTHY: '#10b981',
        WARNING: '#f59e0b',
        ERROR:   '#ef4444',
    };

    function layoutNodes(modules) {
        const cols = Math.ceil(Math.sqrt(modules.length || 1));
        return modules.map((m, i) => ({
            id: m.id,
            type: 'default',
            data: { label: m.name || m.id, status: m.status, beanCount: m.beanCount },
            position: { x: (i % cols) * 220 + 60, y: Math.floor(i / cols) * 160 + 60 },
            style: {
                background: STATUS_COLORS[m.status] || '#6b7280',
                color: 'white',
                padding: '10px 16px',
                borderRadius: '6px',
                border: 'none',
                fontWeight: '600',
                minWidth: '120px',
                textAlign: 'center',
            },
        }));
    }

    function buildEdges(edges) {
        const seen = new Set();
        return edges
            .filter(e => e.fromModuleId && e.toModuleId)
            .map(e => {
                const key = `${e.fromModuleId}-${e.toModuleId}-${e.edgeType}`;
                if (seen.has(key)) return null;
                seen.add(key);
                return {
                    id: e.id || key,
                    source: e.fromModuleId,
                    target: e.toModuleId,
                    label: e.edgeType,
                    animated: e.edgeType === 'PUBLISHES',
                    style: { stroke: '#9ca3af' },
                    labelStyle: { fontSize: 11, fill: '#6b7280' },
                };
            })
            .filter(Boolean);
    }

    function DetailPanel({ node, onClose }) {
        if (!node) return null;
        const { label, status, beanCount } = node.data;
        return (
            React.createElement('div', { className: 'detail-panel' },
                React.createElement('div', { className: 'detail-panel-header' },
                    React.createElement('h3', null, label),
                    React.createElement('button', { className: 'close-btn', onClick: onClose }, '×')
                ),
                React.createElement('div', { className: 'detail-panel-body' },
                    React.createElement('div', { className: 'detail-row' },
                        React.createElement('span', { className: 'detail-label' }, 'Status'),
                        React.createElement('span', {
                            className: 'status-badge',
                            style: { background: STATUS_COLORS[status] || '#6b7280' },
                        }, status)
                    ),
                    React.createElement('div', { className: 'detail-row' },
                        React.createElement('span', { className: 'detail-label' }, 'Beans'),
                        React.createElement('span', null, beanCount ?? '—')
                    )
                )
            )
        );
    }

    function Header({ loading, lastUpdated, moduleCount, eventCount }) {
        const fmt = lastUpdated
            ? lastUpdated.toLocaleTimeString()
            : '—';
        return (
            React.createElement('div', { className: 'header' },
                React.createElement('div', { className: 'header-brand' },
                    React.createElement('span', { className: 'header-title' }, 'Eventus'),
                    React.createElement('span', { className: 'header-tagline' }, 'Event Topology')
                ),
                React.createElement('div', { className: 'header-stats' },
                    React.createElement('span', { className: 'stat' }, moduleCount, ' modules'),
                    React.createElement('span', { className: 'stat' }, eventCount, ' events'),
                    loading
                        ? React.createElement('span', { className: 'refreshing' }, '↻ refreshing…')
                        : React.createElement('span', { className: 'last-updated' }, 'Updated ', fmt)
                )
            )
        );
    }

    function EventusApp() {
        const [nodes, setNodes, onNodesChange] = useNodesState([]);
        const [edges, setEdges, onEdgesChange] = useEdgesState([]);
        const [loading, setLoading] = useState(true);
        const [refreshing, setRefreshing] = useState(false);
        const [selectedNode, setSelectedNode] = useState(null);
        const [lastUpdated, setLastUpdated] = useState(null);
        const [stats, setStats] = useState({ modules: 0, events: 0 });
        const [error, setError] = useState(null);

        const fetchGraph = useCallback(async (initial) => {
            if (initial) setLoading(true); else setRefreshing(true);
            try {
                const res = await fetch('/eventus/api/graph');
                if (!res.ok) throw new Error('HTTP ' + res.status);
                const data = await res.json();
                setNodes(layoutNodes(data.modules || []));
                setEdges(buildEdges(data.edges || []));
                setStats({ modules: (data.modules || []).length, events: (data.events || []).length });
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

        const onNodeClick = useCallback((_evt, node) => setSelectedNode(node), []);

        if (loading) {
            return React.createElement('div', { className: 'splash' },
                React.createElement('div', { className: 'spinner' }),
                React.createElement('p', null, 'Loading event topology…')
            );
        }

        if (error && nodes.length === 0) {
            return React.createElement('div', { className: 'splash error' },
                React.createElement('p', null, 'Could not load graph: ', error)
            );
        }

        return (
            React.createElement('div', { className: 'app' },
                React.createElement(Header, {
                    loading: refreshing,
                    lastUpdated,
                    moduleCount: stats.modules,
                    eventCount: stats.events,
                }),
                React.createElement('div', { className: 'canvas' },
                    React.createElement(Flow, {
                        nodes,
                        edges,
                        onNodesChange,
                        onEdgesChange,
                        onNodeClick,
                        fitView: true,
                        attributionPosition: 'bottom-left',
                    },
                        React.createElement(Background, { gap: 20, color: '#e5e7eb' }),
                        React.createElement(Controls),
                        React.createElement(MiniMap, {
                            nodeColor: n => STATUS_COLORS[n.data.status] || '#6b7280',
                            maskColor: 'rgba(249,250,251,0.7)',
                        })
                    ),
                    React.createElement(DetailPanel, {
                        node: selectedNode,
                        onClose: () => setSelectedNode(null),
                    })
                )
            )
        );
    }

    const root = document.getElementById('root');
    ReactDOM.createRoot(root).render(
        React.createElement(ReactFlowProvider, null,
            React.createElement(EventusApp)
        )
    );
}());
