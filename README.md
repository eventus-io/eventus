# Eventus

**Event topology, made visible.**

[![Build](https://github.com/eventus-io/eventus/actions/workflows/build.yml/badge.svg)](https://github.com/eventus-io/eventus/actions/workflows/build.yml)
[![Modulith Compatibility](https://github.com/eventus-io/eventus/actions/workflows/compatibility.yml/badge.svg)](https://github.com/eventus-io/eventus/actions/workflows/compatibility.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](LICENSE)

Eventus extracts the event and module topology from your JVM application,
materialises it as a live knowledge graph, and exposes it through an embedded
dashboard, Spring Boot Actuator endpoints, and an MCP server for LLM queries.

Start with Spring Modulith — expand to Spring Cloud Stream, Kafka, and plain
JVM applications as your needs grow.

---

## Modules

| Module              | Description                                                             |
|---------------------|-------------------------------------------------------------------------|
| `eventus-core`      | Framework-agnostic interfaces and in-memory backend                     |
| `eventus-spring`    | Spring Modulith extractor, actuator endpoints, impact analysis, violations, drift detection |
| `eventus-generic`   | Annotation-based extractor for plain JVM apps                           |
| `eventus-streams`   | Spring Cloud Stream extractor (Kafka, RabbitMQ, etc.)                   |
| `eventus-ui`        | Embedded React dashboard — module graph, event flows, publication log   |
| `eventus-mcp`       | MCP server exposing the graph as LLM-callable tools                     |
| `eventus-neo4j`     | Neo4j graph backend (optional persistence layer)                        |

---

## Quick Start

Add to your Spring Modulith project:

```xml
<dependency>
  <groupId>io.eventus</groupId>
  <artifactId>eventus-spring</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
<!-- optional: embedded dashboard -->
<dependency>
  <groupId>io.eventus</groupId>
  <artifactId>eventus-ui</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Expose the endpoints in `application.properties`:

```properties
management.endpoints.web.exposure.include=*
eventus.ui.enabled=true
```

Start your application and open the dashboard:

```
http://localhost:8080/eventus
```

Or query the actuator endpoints directly:

```bash
curl http://localhost:8080/actuator/eventus-modules
curl http://localhost:8080/actuator/eventus-events
curl http://localhost:8080/actuator/eventus-publications
```

---

## Embedded Dashboard

`eventus-ui` bundles a React dashboard served at `/eventus`. It shows:

- **Module graph** — nodes for each module, arrows for each event flow (click a node to inspect its published and consumed events)
- **Impact analysis** — which modules are affected if an event changes
- **Violations** — hidden couplings and missing dependency declarations
- **Drift detection** — topology changes since the last saved baseline
- **Publication log** — incomplete or stale transactional event publications

---

## Actuator Endpoints

| Endpoint                              | Description                                   |
|---------------------------------------|-----------------------------------------------|
| `GET /actuator/eventus-modules`       | All modules with status and bean count        |
| `GET /actuator/eventus-events`        | All domain events with publisher info         |
| `GET /actuator/eventus-publications`  | Incomplete and stale event publications       |

### REST API

| Endpoint                                        | Description                                 |
|-------------------------------------------------|---------------------------------------------|
| `GET /eventus/api/graph`                        | Full topology (modules, events, edges, publications) |
| `GET /eventus/api/impact/event/{eventId}`       | Which modules are affected by an event      |
| `GET /eventus/api/impact/module/{moduleId}`     | Events published by a module and downstream listeners |
| `GET /eventus/api/violations`                   | Detected topology violations (filterable by severity/type) |
| `GET /eventus/api/drift`                        | Topology drift since last baseline          |
| `POST /eventus/api/drift/baseline`              | Capture the current topology as baseline    |

---

## MCP Server

`eventus-mcp` exposes the graph as tools for LLM agents via the Model Context Protocol.

```xml
<dependency>
  <groupId>io.eventus</groupId>
  <artifactId>eventus-mcp</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Protect the `/mcp/**` endpoint with an API key:

```properties
eventus.mcp.api-key=your-secret-key
```

---

## Spring Cloud Stream

`eventus-streams` extracts producer/consumer topology from Spring Cloud Stream bindings — no code changes needed.

```xml
<dependency>
  <groupId>io.eventus</groupId>
  <artifactId>eventus-streams</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

---

## Configuration

| Property                               | Default  | Description                                         |
|----------------------------------------|----------|-----------------------------------------------------|
| `eventus.enabled`                      | `true`   | Enable/disable Eventus entirely                     |
| `eventus.publications.stale-threshold` | `PT2H`   | Duration before an incomplete publication is stale  |
| `eventus.ui.enabled`                   | `true`   | Enable/disable the embedded dashboard               |
| `eventus.mcp.enabled`                  | `true`   | Enable/disable the MCP server                       |
| `eventus.mcp.api-key`                  | —        | API key required to call `/mcp/**` (unset = open)   |

---

## Example

A runnable Spring Modulith bookstore example is in [`examples/spring-modulith`](examples/spring-modulith).

```bash
# Build Eventus locally first
mvn install -DskipTests

# Run the example
cd examples/spring-modulith
mvn spring-boot:run
```

Then open `http://localhost:8080/eventus` to see the module graph.

---

## Compatibility

| Eventus | Spring Boot | Spring Modulith | Java |
|---------|-------------|-----------------|------|
| 0.1.x   | 4.0.x       | 2.0.x           | 25   |

The compatibility matrix across Spring Modulith minor versions is verified automatically on every push via [`compatibility.yml`](.github/workflows/compatibility.yml).

---

## Roadmap

| Version | Scope                                                                        |
|---------|------------------------------------------------------------------------------|
| v0.1    | Spring Modulith extractor, actuator endpoints, embedded UI, MCP server       |
| v0.2    | Kafka / Axon extractors, Neo4j persistence, multi-app topology federation    |
| v0.3    | Grafana dashboard export, alerting on drift, AI-assisted impact summaries    |

---

## Releasing to Maven Central

### Required GitHub secrets

| Secret           | How to obtain                                    |
|------------------|--------------------------------------------------|
| `GPG_PRIVATE_KEY` | `gpg --export-secret-keys --armor <KEY_ID>`     |
| `GPG_PASSPHRASE`  | Passphrase for the GPG key                       |
| `OSSRH_USERNAME`  | Sonatype OSSRH username or user token            |
| `OSSRH_TOKEN`     | Sonatype OSSRH password or token                 |

### Triggering a release

```bash
mvn versions:set -DnewVersion=0.2.0
git add -A && git commit -m "chore: release 0.2.0"
git tag v0.2.0
git push origin main --tags
```

The `release.yml` workflow signs all artifacts with GPG and deploys to Maven Central via the Nexus staging plugin.

---

## Contributing

See [CONTRIBUTING.md](.github/CONTRIBUTING.md).

---

## License

Apache 2.0 — see [LICENSE](LICENSE).
