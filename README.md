# Eventus

**Event topology, made visible.**

[![Build](https://github.com/eventus-io/eventus/actions/workflows/build.yml/badge.svg)](https://github.com/eventus-io/eventus/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](LICENSE)

Eventus extracts the event and module topology from your JVM application,
materialises it as a live knowledge graph, and exposes it through Spring Boot
Actuator endpoints. Start with Spring Modulith — expand to Kafka, Axon, and
plain JVM applications as your needs grow.

---

## Modules

| Module             | Description                                              |
|--------------------|----------------------------------------------------------|
| `eventus-core`     | Framework-agnostic interfaces and in-memory backend      |
| `eventus-spring`   | Spring Modulith extractor + actuator endpoints           |
| `eventus-generic`  | Annotation-based extractor for plain JVM apps (v0.3)    |

---

## Quick Start

Add to your Spring Modulith project:

```xml
<dependency>
  <groupId>io.eventus</groupId>
  <artifactId>eventus-spring</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Expose the endpoints in `application.properties`:

```properties
management.endpoints.web.exposure.include=health,info,eventus-modules,eventus-events,eventus-publications
```

Start your application and query:

```bash
# All modules with health status
curl http://localhost:8080/actuator/eventus-modules

# All domain events with publisher info
curl http://localhost:8080/actuator/eventus-events

# Incomplete and stale event publications
curl http://localhost:8080/actuator/eventus-publications
```

---

## Endpoints

| Endpoint                                 | Description                                |
|------------------------------------------|--------------------------------------------|
| `GET /actuator/eventus-modules`          | All modules with status and bean count     |
| `GET /actuator/eventus-events`           | All domain events with publisher info      |
| `GET /actuator/eventus-publications`     | Incomplete and stale event publications    |

### Example: `/actuator/eventus-modules`

```json
[
  { "id": "order", "name": "Order", "beanCount": 4, "aggregateCount": 1, "status": "HEALTHY" },
  { "id": "inventory", "name": "Inventory", "beanCount": 2, "aggregateCount": 0, "status": "HEALTHY" }
]
```

### Example: `/actuator/eventus-events`

```json
[
  { "id": "com.example.order.OrderPlaced", "name": "OrderPlaced", "publisherModuleId": "order" }
]
```

---

## Configuration

| Property                                 | Default  | Description                                      |
|------------------------------------------|----------|--------------------------------------------------|
| `eventus.enabled`                        | `true`   | Enable/disable Eventus entirely                  |
| `eventus.publications.stale-threshold`   | `PT2H`   | Duration before an incomplete publication is stale |

---

## Roadmap

| Version | Scope                                                               |
|---------|---------------------------------------------------------------------|
| v0.1    | Core interfaces + Spring Modulith extractor + actuator endpoints    |
| v0.2    | Embedded React UI (module graph view + publication log)             |
| v0.3    | Grafana dashboard + Micrometer metrics + Kafka extractor            |
| v0.4    | Impact analysis API + violation detection + drift detection         |
| v1.0    | MCP server + LLM query panel + Axon + generic extractors            |

---

## Contributing

See [CONTRIBUTING.md](.github/CONTRIBUTING.md).

Built with Spring Modulith in mind, designed for the full JVM ecosystem.

---

## License

Apache 2.0 — see [LICENSE](LICENSE).
