# S12 — Grafana Dashboard & Micrometer Metrics

## Goal
Expose Eventus topology data as Micrometer metrics, then provide a pre-built Grafana dashboard JSON for platform teams to import.

## Acceptance Criteria
- [ ] Micrometer metrics registered for modules, events, publications
- [ ] Metrics exposed on actuator `/actuator/prometheus` endpoint
- [ ] Grafana dashboard JSON provided and documented
- [ ] Dashboard shows: module count, event count, publication health, stale publication aging
- [ ] Metrics include: `eventus.modules.total`, `eventus.events.total`, `eventus.publications.incomplete`, `eventus.publications.stale`
- [ ] Dashboard auto-refreshes every 30 seconds
- [ ] All tests green, dashboard renders in Grafana without errors

## Metrics Design

### Gauge Metrics (refresh every collection cycle)

```java
MeterRegistry.gauge("eventus.modules.total", () -> graphReader.getModules().size());
MeterRegistry.gauge("eventus.modules.healthy", () -> 
    graphReader.getModules().stream()
        .filter(m -> m.status() == ModuleStatus.HEALTHY).count());
MeterRegistry.gauge("eventus.modules.warning", () -> 
    graphReader.getModules().stream()
        .filter(m -> m.status() == ModuleStatus.WARNING).count());
MeterRegistry.gauge("eventus.modules.error", () -> 
    graphReader.getModules().stream()
        .filter(m -> m.status() == ModuleStatus.ERROR).count());

MeterRegistry.gauge("eventus.events.total", () -> graphReader.getEvents().size());

MeterRegistry.gauge("eventus.publications.incomplete", () -> 
    graphReader.getPublications().stream()
        .filter(p -> p.status() == PublicationStatus.INCOMPLETE).count());
MeterRegistry.gauge("eventus.publications.stale", () -> 
    graphReader.getPublications().stream()
        .filter(p -> p.status() == PublicationStatus.STALE).count());
MeterRegistry.gauge("eventus.publications.completed", () -> 
    graphReader.getPublications().stream()
        .filter(p -> p.status() == PublicationStatus.COMPLETED).count());
```

### Metrics Collector Service

```java
// io.eventus.spring.metrics/EventusMetricsCollector.java
@Component
public class EventusMetricsCollector {
    private final MeterRegistry meterRegistry;
    private final GraphReader graphReader;

    public EventusMetricsCollector(MeterRegistry meterRegistry, GraphReader graphReader) {
        this.meterRegistry = meterRegistry;
        this.graphReader = graphReader;
        registerMetrics();
    }

    private void registerMetrics() {
        meterRegistry.gauge("eventus.modules.total",
            () -> graphReader.getModules().size());
        
        meterRegistry.gauge("eventus.modules.healthy",
            () -> graphReader.getModules().stream()
                .filter(m -> m.status() == ModuleStatus.HEALTHY)
                .count());
        
        meterRegistry.gauge("eventus.modules.warning",
            () -> graphReader.getModules().stream()
                .filter(m -> m.status() == ModuleStatus.WARNING)
                .count());
        
        meterRegistry.gauge("eventus.modules.error",
            () -> graphReader.getModules().stream()
                .filter(m -> m.status() == ModuleStatus.ERROR)
                .count());
        
        meterRegistry.gauge("eventus.events.total",
            () -> graphReader.getEvents().size());
        
        meterRegistry.gauge("eventus.publications.incomplete",
            () -> graphReader.getPublications().stream()
                .filter(p -> p.status() == PublicationStatus.INCOMPLETE)
                .count());
        
        meterRegistry.gauge("eventus.publications.stale",
            () -> graphReader.getPublications().stream()
                .filter(p -> p.status() == PublicationStatus.STALE)
                .count());
        
        meterRegistry.gauge("eventus.publications.completed",
            () -> graphReader.getPublications().stream()
                .filter(p -> p.status() == PublicationStatus.COMPLETED)
                .count());
    }
}
```

### Update Auto-Configuration

```java
// In EventusAutoConfiguration
@Bean
@ConditionalOnMissingBean
public EventusMetricsCollector eventusMetricsCollector(
        MeterRegistry meterRegistry, 
        GraphReader graphReader) {
    return new EventusMetricsCollector(meterRegistry, graphReader);
}
```

## Grafana Dashboard JSON

Create `eventus-spring/docs/grafana-dashboard.json`:

```json
{
  "dashboard": {
    "title": "Eventus — Event Topology",
    "description": "Monitor event-driven application health, module status, and publication reliability",
    "tags": ["eventus", "spring-modulith", "events"],
    "timezone": "browser",
    "refresh": "30s",
    "panels": [
      {
        "title": "Modules — Total",
        "type": "stat",
        "targets": [
          {
            "expr": "eventus_modules_total"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "custom": {},
            "color": { "mode": "palette-classic" }
          }
        }
      },
      {
        "title": "Module Health",
        "type": "piechart",
        "targets": [
          {
            "expr": "eventus_modules_healthy",
            "legendValues": ["Healthy"]
          },
          {
            "expr": "eventus_modules_warning",
            "legendValues": ["Warning"]
          },
          {
            "expr": "eventus_modules_error",
            "legendValues": ["Error"]
          }
        ]
      },
      {
        "title": "Events — Total",
        "type": "stat",
        "targets": [
          {
            "expr": "eventus_events_total"
          }
        ]
      },
      {
        "title": "Publication Status",
        "type": "bargauge",
        "targets": [
          {
            "expr": "eventus_publications_completed",
            "legendValues": ["Completed"]
          },
          {
            "expr": "eventus_publications_incomplete",
            "legendValues": ["Incomplete"]
          },
          {
            "expr": "eventus_publications_stale",
            "legendValues": ["Stale"]
          }
        ],
        "fieldConfig": {
          "defaults": {
            "custom": {
              "orientation": "horizontal"
            }
          }
        }
      }
    ]
  }
}
```

## Documentation

Create `eventus-spring/docs/GRAFANA_SETUP.md`:

```markdown
# Grafana Integration

## Prerequisites
- Prometheus scraping your Spring Boot app at `/actuator/prometheus`
- Grafana v7.0+

## Import Dashboard

1. Open Grafana → Dashboards → New → Import
2. Paste contents of `grafana-dashboard.json`
3. Select Prometheus as data source
4. Click Import

## Metrics Reference

| Metric | Type | Description |
|--------|------|-------------|
| eventus_modules_total | Gauge | Total modules in application |
| eventus_modules_healthy | Gauge | Modules with HEALTHY status |
| eventus_modules_warning | Gauge | Modules with WARNING status |
| eventus_modules_error | Gauge | Modules with ERROR status |
| eventus_events_total | Gauge | Total unique domain events |
| eventus_publications_completed | Gauge | Completed event publications |
| eventus_publications_incomplete | Gauge | Incomplete event publications |
| eventus_publications_stale | Gauge | Stale (aged) event publications |

## Alerts (Example)

Add to Prometheus rules:

```yaml
groups:
  - name: eventus
    rules:
      - alert: EventusStalePublications
        expr: eventus_publications_stale > 0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "{{ $value }} stale event publications detected"
```
```

## Tests Required

```java
// eventus-spring/src/test/java/io/eventus/spring/metrics/EventusMetricsCollectorTest.java
@SpringBootTest
class EventusMetricsCollectorTest {
    @Autowired MeterRegistry meterRegistry;
    @Autowired GraphReader graphReader;

    @Test
    void metricsAreRegistered() {
        assertThat(meterRegistry.find("eventus.modules.total").gauge()).isPresent();
        assertThat(meterRegistry.find("eventus.events.total").gauge()).isPresent();
        assertThat(meterRegistry.find("eventus.publications.incomplete").gauge()).isPresent();
        assertThat(meterRegistry.find("eventus.publications.stale").gauge()).isPresent();
    }

    @Test
    void moduleTotalMetricReturnsCorrectCount() {
        Gauge gauge = meterRegistry.find("eventus.modules.total").gauge().orElse(null);
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(graphReader.getModules().size());
    }

    @Test
    void publicationMetricsUpdateCorrectly() {
        Gauge incomplete = meterRegistry.find("eventus.publications.incomplete").gauge().orElse(null);
        assertThat(incomplete).isNotNull();
        // Verify value matches actual incomplete count
        long expectedIncomplete = graphReader.getPublications().stream()
            .filter(p -> p.status() == PublicationStatus.INCOMPLETE)
            .count();
        assertThat(incomplete.value()).isEqualTo(expectedIncomplete);
    }
}

// eventus-spring/src/test/java/io/eventus/spring/metrics/PrometheusEndpointTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class PrometheusEndpointTest {
    @Autowired MockMvc mockMvc;

    @Test
    void prometheusEndpointExposesEventusMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("eventus_modules_total")))
            .andExpect(content().string(containsString("eventus_events_total")))
            .andExpect(content().string(containsString("eventus_publications_incomplete")));
    }
}
```

## Configuration

Update `application.properties`:

```properties
# Enable Prometheus endpoint
management.endpoints.web.exposure.include=health,info,prometheus,eventus-modules,eventus-events,eventus-publications
management.metrics.export.prometheus.enabled=true
```

## Done When
- Metrics collector bean created and auto-configured
- All metrics exposed via `/actuator/prometheus`
- Grafana dashboard JSON provided and validated
- Tests verify metrics are accurate
- Documentation complete with setup steps and metric reference
- README updated with Grafana section and link to dashboard JSON
