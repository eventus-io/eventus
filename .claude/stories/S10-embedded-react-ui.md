# S10 — Embedded React UI (Module Graph Visualization)

## Goal
Build a self-hosted React dashboard served from Spring Boot that visualizes the module topology as an interactive graph. Zero external dependencies for runtime.

## Acceptance Criteria
- [ ] React app serves from Spring Boot at `/eventus/ui`
- [ ] Graph visualization with React Flow (force-directed layout)
- [ ] Module nodes colored by status (HEALTHY=green, WARNING=yellow, ERROR=red)
- [ ] Edge labels show relationship type (PUBLISHES, LISTENS_TO, DEPENDS_ON)
- [ ] Clicking a module shows detailed panel: bean count, aggregates, status, listeners
- [ ] Clicking an event shows list of publishers/listeners with counts
- [ ] Responsive design: works on desktop and tablet
- [ ] Build process: Maven build bundles React → static assets → JAR
- [ ] Auto-refresh graph every 5 seconds (configurable via `eventus.ui.refresh-interval`)
- [ ] Loading indicator while data fetches
- [ ] No external API calls beyond CDN for libraries

## Architecture

### Backend: New REST Controller

```java
// io.eventus.spring.ui/EventusUIApiController.java
@RestController
@RequestMapping("/eventus/api")
public class EventusUIApiController {
    private final GraphReader graphReader;
    
    public EventusUIApiController(GraphReader graphReader) {
        this.graphReader = graphReader;
    }
    
    @GetMapping("/graph")
    public GraphResponse getGraph() {
        return new GraphResponse(
            graphReader.getModules(),
            graphReader.getEvents(),
            graphReader.getEdges(),
            graphReader.getPublications()
        );
    }
}

public record GraphResponse(
    List<ModuleNode> modules,
    List<EventNode> events,
    List<EventEdge> edges,
    List<PublicationRecord> publications
) {}
```

### Frontend Serving

```java
// Update EventusAutoConfiguration
@Bean
@ConditionalOnMissingBean
public EventusUIApiController eventusUIApiController(GraphReader reader) {
    return new EventusUIApiController(reader);
}
```

Spring Boot automatically serves `src/main/resources/static/index.html` at `/`, and static files from that directory. Create:

```
eventus-spring/src/main/resources/static/
├── index.html
├── eventus-ui.js      (React components)
├── eventus-ui.css     (styling)
└── favicon.ico
```

### React Component

Use React from CDN (no build step needed). `eventus-ui.js` is plain JS that loads React + React Flow:

```javascript
// eventus-ui.js (simplified)
const { useState, useEffect } = React;
const { ReactFlow, Background, Controls, Handle, Position } = window.ReactFlowPackage;

// Transform API response to React Flow nodes/edges
function transformGraph(data) {
    const nodes = data.modules.map(m => ({
        id: m.id,
        data: { label: m.name, status: m.status, beanCount: m.beanCount },
        position: { x: Math.random() * 500, y: Math.random() * 500 },
        style: {
            background: m.status === 'HEALTHY' ? '#10b981' : m.status === 'WARNING' ? '#f59e0b' : '#ef4444',
            color: 'white',
            padding: '10px',
            borderRadius: '4px',
        }
    }));
    
    const edges = data.edges.map(e => ({
        id: e.id,
        source: e.fromModuleId,
        target: e.toModuleId,
        label: e.edgeType,
        animated: e.edgeType === 'PUBLISHES',
    }));
    
    return { nodes, edges };
}

function EventusUI() {
    const [nodes, setNodes] = useState([]);
    const [edges, setEdges] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedNode, setSelectedNode] = useState(null);

    const fetchGraph = async () => {
        try {
            const res = await fetch('/eventus/api/graph');
            const data = await res.json();
            const { nodes, edges } = transformGraph(data);
            setNodes(nodes);
            setEdges(edges);
            setLoading(false);
        } catch (err) {
            console.error('Failed to fetch graph:', err);
        }
    };

    useEffect(() => {
        fetchGraph();
        const interval = setInterval(fetchGraph, 5000);
        return () => clearInterval(interval);
    }, []);

    const handleNodeClick = (event, node) => {
        setSelectedNode(node);
    };

    if (loading) return <div className="loading">Loading graph...</div>;

    return (
        <div className="eventus-container">
            <ReactFlow nodes={nodes} edges={edges} onNodeClick={handleNodeClick}>
                <Background />
                <Controls />
            </ReactFlow>
            {selectedNode && (
                <div className="detail-panel">
                    <h3>{selectedNode.data.label}</h3>
                    <p>Status: {selectedNode.data.status}</p>
                    <p>Beans: {selectedNode.data.beanCount}</p>
                    <button onClick={() => setSelectedNode(null)}>Close</button>
                </div>
            )}
        </div>
    );
}

ReactDOM.createRoot(document.getElementById('root')).render(<EventusUI />);
```

### HTML Entry Point

```html
<!-- index.html -->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Eventus — Event Topology</title>
    <script src="https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/reactflow@11/dist/umd/reactflow.production.min.js"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/reactflow@11/dist/style.css">
    <link rel="stylesheet" href="/eventus-ui.css">
</head>
<body>
    <div id="root"></div>
    <script src="/eventus-ui.js"></script>
</body>
</html>
```

### Styling

```css
/* eventus-ui.css */
* { margin: 0; padding: 0; box-sizing: border-box; }

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: #f9fafb;
}

.eventus-container {
    width: 100vw;
    height: 100vh;
    display: flex;
    position: relative;
}

.react-flow {
    flex: 1;
}

.detail-panel {
    position: absolute;
    bottom: 20px;
    right: 20px;
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 16px;
    width: 300px;
    box-shadow: 0 4px 6px rgba(0,0,0,0.1);
    z-index: 10;
}

.loading {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    font-size: 18px;
    color: #6b7280;
}
```

## Configuration

Update `EventusProperties`:

```java
@ConfigurationProperties(prefix = "eventus")
public class EventusProperties {
    private boolean enabled = true;
    private Publications publications = new Publications();
    private UI ui = new UI();
    
    public static class Publications {
        private Duration staleThreshold = Duration.ofHours(2);
        // getters/setters
    }
    
    public static class UI {
        private boolean enabled = true;
        private Duration refreshInterval = Duration.ofSeconds(5);
        // getters/setters
    }
}
```

## Tests Required

```java
// src/test/java/io/eventus/spring/ui/EventusUIApiControllerTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class EventusUIApiControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired GraphReader graphReader;

    @Test
    void graphEndpointReturnsValidResponse() throws Exception {
        mockMvc.perform(get("/eventus/api/graph"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.modules", isArray()))
            .andExpect(jsonPath("$.events", isArray()))
            .andExpect(jsonPath("$.edges", isArray()))
            .andExpect(jsonPath("$.publications", isArray()));
    }
}

// s/test/java/io/eventus/spring/EventusUIIntegrationTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class EventusUIIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void uiLoadsAtRootPath() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/html;charset=UTF-8"));
    }

    @Test
    void staticAssetsServedCorrectly() throws Exception {
        mockMvc.perform(get("/eventus-ui.js"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/javascript"));
    }
}
```

## Performance Constraints
- API response time: <100ms for up to 100 modules and 1000 events
- Initial page load: <2s on 4G
- Graph update after refresh: <500ms

## Done When
- `http://localhost:8080/` renders an interactive graph with module nodes
- Nodes are colored by status
- Edges labeled with relationship type
- Clicking a module shows a detail panel (name, status, bean count)
- Auto-refresh works every 5 seconds
- `mvn verify -pl eventus-spring` passes all tests
- README updated with UI screenshot and quick-start instructions
