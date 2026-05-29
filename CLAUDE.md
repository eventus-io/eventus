# Eventus — Claude Code Context

> Event topology, made visible.

## Project Overview

Eventus is an open source JVM library that extracts the event and module topology
from a running application, materialises it as a knowledge graph, and exposes it
through actuator endpoints and an MCP server for LLM queries. A standalone React
dashboard (`ui/`) connects to those endpoints and can be deployed independently.

## Coordinates

| Property       | Value                          |
|----------------|-------------------------------|
| Maven group    | io.eventus                    |
| GitHub         | github.com/rafaelmaia/eventus |
| License        | Apache 2.0                    |
| Java           | 25                            |
| Spring Boot    | 4.0.6                         |

## Module Structure

| Module             | Purpose                                                        |
|--------------------|----------------------------------------------------------------|
| `eventus-core`     | Framework-agnostic interfaces, value objects, in-memory backend |
| `eventus-spring`   | Spring Modulith extractor + actuator endpoints + autoconfiguration |
| `eventus-generic`  | Classpath-scan extractor via `@EventModule`/`@Publishes`/`@Listens` annotations |
| `eventus-streams`  | Spring Cloud Stream binding extractor                          |
| `eventus-neo4j`    | Neo4j-backed GraphWriter/GraphReader                           |
| `eventus-mcp`      | MCP server (Spring AI) exposing graph as LLM tools             |

### Standalone frontend

| Directory | Purpose                                                               |
|-----------|-----------------------------------------------------------------------|
| `ui/`     | React + TypeScript + Vite dashboard — standalone, Docker-deployable  |

- `npm run dev` starts the dev server with a proxy to `localhost:8080`
- `docker build` produces an nginx image; set `EVENTUS_API_URL` at runtime

## Tagline

**"Event topology, made visible."**

## How to Work on This Project

Stories live in `.claude/stories/`. Each story is self-contained with
context, acceptance criteria, and implementation notes. Work one story
at a time. All tests must be green before moving to the next story.

## Coding Conventions

- Java 21 with records for value objects
- Constructor injection, no field injection
- Package: `io.eventus.<module>`
- Tests: JUnit 5 + AssertJ + Mockito
- Conventional commits: `feat:`, `fix:`, `test:`, `docs:`
- No Lombok

## Current Milestone

**v0.1** — Core interfaces + Spring Modulith extractor + actuator endpoints

Stories to complete (in order):
1. `S01-project-scaffold.md`
2. `S02-core-interfaces.md`
3. `S03-in-memory-backend.md`
4. `S04-spring-modulith-extractor.md`
5. `S05-actuator-endpoints.md`
6. `S06-spring-autoconfiguration.md`
7. `S07-integration-test.md`
8. `S08-readme-and-ci.md`
9. `S09-github-push.md`
