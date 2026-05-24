# Grafana Integration

## Prerequisites

- Prometheus scraping your Spring Boot app at `/actuator/prometheus`
- Grafana v7.0+
- `micrometer-registry-prometheus` on the classpath

## Enable Prometheus Endpoint

Add to `application.properties`:

```properties
management.endpoints.web.exposure.include=health,info,prometheus,eventus-modules,eventus-events,eventus-publications
```

Add to `pom.xml`:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

## Import Dashboard

1. Open Grafana → **Dashboards → New → Import**
2. Paste contents of `grafana-dashboard.json`
3. Select your Prometheus data source
4. Click **Import**

## Metrics Reference

| Metric                          | Type  | Description                            |
|---------------------------------|-------|----------------------------------------|
| `eventus_modules_total`         | Gauge | Total modules in the application       |
| `eventus_modules_healthy`       | Gauge | Modules with HEALTHY status            |
| `eventus_modules_warning`       | Gauge | Modules with WARNING status            |
| `eventus_modules_error`         | Gauge | Modules with ERROR status              |
| `eventus_events_total`          | Gauge | Total unique domain events             |
| `eventus_publications_completed`| Gauge | Completed event publications           |
| `eventus_publications_incomplete`| Gauge| Incomplete event publications          |
| `eventus_publications_stale`    | Gauge | Stale (aged past threshold) publications |

## Alerting Example

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
      - alert: EventusModuleErrors
        expr: eventus_modules_error > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "{{ $value }} modules in ERROR state"
```
