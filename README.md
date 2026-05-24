# Eventus

**Event topology, made visible.**

[![Build](https://github.com/RafMaia92/eventus/actions/workflows/build.yml/badge.svg)](https://github.com/RafMaia92/eventus/actions/workflows/build.yml)
[![Modulith Compatibility](https://github.com/RafMaia92/eventus/actions/workflows/compatibility.yml/badge.svg)](https://github.com/RafMaia92/eventus/actions/workflows/compatibility.yml)
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

## Compatibility

| Eventus | Spring Modulith | Spring Boot |
|---------|-----------------|-------------|
| 0.1.x   | 1.2.x, 1.3.x    | 3.2.x, 3.4.x |
| 1.0.x   | 1.3.x, 2.0.x    | 3.3.x, 3.4.x |

The matrix is verified automatically on every push via `compatibility.yml`.

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

## Releasing to Maven Central

### Required GitHub secrets

| Secret | How to obtain |
|---|---|
| `GPG_PRIVATE_KEY` | `gpg --export-secret-keys --armor <KEY_ID>` |
| `GPG_PASSPHRASE` | Passphrase for the GPG key |
| `OSSRH_USERNAME` | Sonatype OSSRH username or user token |
| `OSSRH_TOKEN` | Sonatype OSSRH password or token |

### Triggering a release

```bash
# Bump versions
mvn versions:set -DnewVersion=0.2.0
git add -A && git commit -m "chore: release 0.2.0"
git tag v0.2.0
git push origin main --tags
```

The `release.yml` workflow picks up the tag, signs all artifacts with GPG,
and deploys to Maven Central automatically via the Nexus staging plugin.

### One-time setup

1. Register a Sonatype OSSRH account at `issues.sonatype.org`
2. Open a ticket to claim group ID `io.eventus` pointing to `github.com/RafMaia92/eventus`
3. Generate a user token in the OSSRH Nexus UI and store it as GitHub secrets
4. Upload your GPG public key: `gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>`

---

## Contributing

See [CONTRIBUTING.md](.github/CONTRIBUTING.md).

Built with Spring Modulith in mind, designed for the full JVM ecosystem.

---

## License

Apache 2.0 — see [LICENSE](LICENSE).
