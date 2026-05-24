# Eventus

**Event topology, made visible.**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Build](https://github.com/RafMaia92/eventus/actions/workflows/build.yml/badge.svg)](https://github.com/RafMaia92/eventus/actions/workflows/build.yml)

Eventus is a zero-configuration library that extracts the event topology of your application and exposes it as actuator endpoints. Drop in the dependency, start your app, and immediately see which modules publish which events and which modules listen to them — without writing a single line of configuration.

Built with Spring Modulith in mind, designed for the full JVM ecosystem.

---

## Module structure

| Module | Description |
|---|---|
| `eventus-core` | Framework-free interfaces (`EventGraphExtractor`, `GraphWriter`) and value objects (`GraphModel`, `ModuleNode`, `EventNode`, `EventEdge`) plus in-memory implementations |
| `eventus-spring` | Spring Modulith extractor + Spring Boot Actuator endpoints (`/actuator/eventus/modules`, `/actuator/eventus/events`, `/actuator/eventus/publications`) |
| `eventus-generic` | Stub extractor for non-Spring apps — annotation-based extraction coming in v0.3 |

---

## Quick start

Add the dependency to your Spring Boot + Spring Modulith application:

```xml
<dependency>
  <groupId>io.eventus</groupId>
  <artifactId>eventus-spring</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Expose the endpoint in `application.properties`:

```properties
management.endpoints.web.exposure.include=health,info,eventus
management.endpoint.eventus.enabled=true
```

---

## How it works

1. **Add the dependency** — Eventus auto-configures itself via Spring Boot's auto-configuration mechanism.
2. **Start your app** — On `ApplicationReadyEvent`, Eventus walks your Spring Modulith module graph and records every module, published event, and listener relationship.
3. **Hit the endpoints** — Query the actuator endpoints to explore your event topology:

```bash
# List all modules with health status
curl http://localhost:8080/actuator/eventus/modules

# List all event types
curl http://localhost:8080/actuator/eventus/events

# List incomplete/stale event publications (requires Spring Modulith Events)
curl http://localhost:8080/actuator/eventus/publications
```

---

## Roadmap

| Version | Scope |
|---|---|
| **v0.1** | Spring Modulith extractor, in-memory graph, actuator endpoints |
| **v0.2** | Neo4j `GraphWriter` — persist topology for querying and visualization |
| **v0.3** | `eventus-generic` annotation-based extractor (`@EventPublisher`, `@EventListener`) for non-Modulith Spring apps |
| **v0.4** | MCP server — expose event graph as an AI-readable tool for IDE assistants |
| **v0.5** | Kafka extractor — derive topology from consumer group metadata |
| **v0.6** | Axon Framework extractor — aggregate + saga event mapping |
| **v1.0** | Stable API, dashboards, multi-service federation |

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).
