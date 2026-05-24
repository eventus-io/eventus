# S08 — README & GitHub Actions CI

## Goal
Make the project presentable and automatically verified on every push.
The README is the project's front door — it must be clear enough that a
Spring Modulith developer can go from zero to working actuator endpoints
in under 5 minutes.

## Acceptance Criteria

### README
- [ ] Exists at the root level as `README.md`
- [ ] Contains: name, tagline, one-paragraph description
- [ ] Contains: Apache 2.0 license badge and build status badge
- [ ] Contains: module structure table
- [ ] Contains: quick-start section (Maven snippet + 3 steps)
- [ ] Contains: actuator endpoints reference table
- [ ] Contains: configuration properties table
- [ ] Contains: roadmap table (v0.1 → v1.0)
- [ ] Contains: contributing section pointing to CONTRIBUTING.md
- [ ] Renders cleanly on GitHub (no broken links, no raw HTML issues)

### GitHub Actions
- [ ] Workflow file at `.github/workflows/build.yml`
- [ ] Triggers on: push to `main`, pull_request targeting `main`
- [ ] Uses Java 21 and Maven
- [ ] Caches `~/.m2/repository`
- [ ] Runs `mvn verify` (includes all tests)
- [ ] Reports test results

### Contributing Guide
- [ ] Exists at `.github/CONTRIBUTING.md`
- [ ] Covers: how to build, how to run tests, PR guidelines, commit format

## README Content

```markdown
# Eventus

**Event topology, made visible.**

[![Build](https://github.com/rafaelmaia/eventus/actions/workflows/build.yml/badge.svg)](...)
[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](LICENSE)

Eventus extracts the event and module topology from your JVM application,
materialises it as a live knowledge graph, and exposes it through Spring Boot
Actuator endpoints. Start with Spring Modulith — expand to Kafka, Axon, and
plain JVM applications as your needs grow.

## Modules

| Module             | Description                                              |
|--------------------|----------------------------------------------------------|
| `eventus-core`     | Framework-agnostic interfaces and in-memory backend      |
| `eventus-spring`   | Spring Modulith extractor + actuator endpoints           |
| `eventus-generic`  | Annotation-based extractor for plain JVM apps (v0.3)    |

## Quick Start

Add to your Spring Modulith project:

```xml
<dependency>
  <groupId>io.eventus</groupId>
  <artifactId>eventus-spring</artifactId>
  <version>0.1.0</version>
</dependency>
```

Expose the endpoints in `application.properties`:
```properties
management.endpoints.web.exposure.include=health,info,eventus-modules,eventus-events,eventus-publications
```

Start your application and query:
```bash
curl http://localhost:8080/actuator/eventus-modules
curl http://localhost:8080/actuator/eventus-events
curl http://localhost:8080/actuator/eventus-publications
```

## Endpoints

| Endpoint                          | Description                              |
|-----------------------------------|------------------------------------------|
| `GET /actuator/eventus-modules`   | All modules with status and bean count   |
| `GET /actuator/eventus-events`    | All domain events with publisher info    |
| `GET /actuator/eventus-publications` | Incomplete and stale event publications |

## Configuration

| Property                              | Default | Description                        |
|---------------------------------------|---------|------------------------------------|
| `eventus.enabled`                     | `true`  | Enable/disable Eventus             |
| `eventus.publications.stale-threshold`| `PT2H`  | Duration before publication is stale |

## Roadmap

| Version | Scope                                                        |
|---------|--------------------------------------------------------------|
| v0.1    | Core interfaces + Spring Modulith extractor + actuator endpoints |
| v0.2    | Embedded React UI (module graph view + publication log)      |
| v0.3    | Grafana dashboard + Micrometer metrics + Kafka extractor     |
| v0.4    | Impact analysis API + violation detection + drift detection  |
| v1.0    | MCP server + LLM query panel + Axon + generic extractors     |

## Contributing

See [CONTRIBUTING.md](.github/CONTRIBUTING.md).

## License

Apache 2.0 — see [LICENSE](LICENSE).
```

## GitHub Actions Workflow

```yaml
# .github/workflows/build.yml
name: Build

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build and test
        run: mvn verify --batch-mode --no-transfer-progress

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: '**/target/surefire-reports/*.xml'
```

## CONTRIBUTING.md

```markdown
# Contributing to Eventus

## Build

Requires Java 21 and Maven 3.9+.

```bash
mvn clean install
```

## Test

```bash
mvn verify
```

Run a single module:
```bash
mvn verify -pl eventus-spring
```

## Commit Format

Use conventional commits:
- `feat:` new feature
- `fix:` bug fix
- `test:` test additions or corrections
- `docs:` documentation only
- `chore:` build, CI, tooling

## Pull Requests

- One logical change per PR
- All tests must pass
- Update README if adding a public API or configuration property
- Reference the relevant story number in the PR description (e.g. `S04`)
```

## Done When
- README renders correctly on GitHub with no broken badges or links
- `mvn verify` triggered by pushing to main completes green in GitHub Actions
- CONTRIBUTING.md exists and is linked from README
