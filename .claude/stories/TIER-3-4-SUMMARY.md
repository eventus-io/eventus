# Eventus Tier 3/4 — Production Readiness + Full JVM Coverage

**Status**: Tier 2 (v0.2–0.4) ✅ Storified  
**Current**: Tier 3/4 Stories S16–S24 Ready

---

## Overview

This batch addresses four remaining gaps:

| Phase | Gap | Stories | Priority |
|-------|-----|---------|----------|
| A | GAP-09 Maven Central Publishing | S16, S17 | HIGH — required before any real adoption |
| B | GAP-07 Neo4j Graph Backend | S18, S19, S20 | MEDIUM — production persistence |
| C | GAP-10 MCP Security | S21 | MEDIUM — production safety |
| C | GAP-11 Version Compatibility Matrix | S22 | MEDIUM — long-term maintainability |
| D | GAP-08 Generic JVM Extractor | S23, S24 | LOW — full JVM ecosystem coverage |

---

## Story Roadmap

| Story | Title | Focus | Dependencies |
|-------|-------|-------|--------------|
| S16 | Maven Central: POM Metadata | POM cleanup + source/javadoc plugins | root pom.xml |
| S17 | Maven Central: Release Workflow | GitHub Actions GPG signing + OSSRH deploy | S16 |
| S18 | Neo4j Module Scaffold | New module, empty auto-config | eventus-core |
| S19 | Neo4j GraphWriter + GraphReader | Core persistence implementation | S18, Testcontainers |
| S20 | Neo4j Auto-Configuration + Schema | Bean wiring + index creation + MCP Cypher tool | S19, S18 |
| S21 | MCP Security Filter | API key protection for `/mcp/**` | eventus-mcp |
| S22 | Version Compatibility Matrix | GitHub Actions matrix + README table | eventus-spring |
| S23 | Generic Extractor Annotations | `@EventModule`, `@Publishes`, `@Listens` in core | eventus-core |
| S24 | AnnotationBasedExtractor | ClassGraph scanner + auto-configuration | S23 |

---

## Key Design Decisions

1. **Maven Central first**: Nothing else matters if teams can't `<dependency>` Eventus.
2. **Neo4j replaces, not supplements**: When Neo4j is on the classpath it takes `@Primary` — no dual-backend complexity.
3. **MCP security off by default**: Enabling it requires an explicit property — no silent behaviour change.
4. **Compatibility matrix is CI, not documentation**: The README table is derived from what the matrix actually passes.
5. **Generic extractor requires no Spring**: `AnnotationBasedExtractor` runs standalone; Spring auto-config is an optional add-on.

---

## Execution Order

Start Phase A before anything else — teams can't adopt without Maven Central.
Phases B–D are independent and can be parallelized.

```
Phase A (must be first):
  S16 → S17   (POM metadata → release workflow — sequential)

Phase B (independent):
  S18 → S19 → S20   (Neo4j scaffold → impl → auto-config — sequential)

Phase C (independent):
  S21           (MCP security — standalone)
  S22           (compatibility matrix — standalone)

Phase D (last):
  S23 → S24   (annotations → scanner — sequential)
```

---

## Testing Strategy

| Story | Test type | Infrastructure |
|-------|-----------|---------------|
| S16 | Build test | `mvn package -Prelease -Dgpg.skip=true` |
| S17 | Workflow lint | `act` or push a test tag |
| S18 | Compilation test | `mvn compile -pl eventus-neo4j` |
| S19 | Integration test | Testcontainers Neo4j |
| S20 | Spring Boot integration test | Testcontainers Neo4j + `@ServiceConnection` |
| S21 | MockMvc unit test | No infrastructure |
| S22 | GitHub Actions matrix | Real CI run |
| S23 | JUnit reflection test | No infrastructure |
| S24 | JUnit unit test | No infrastructure (no Spring context) |

---

## Common Pitfalls

1. **S16**: `maven-javadoc-plugin` fails on packages with missing `package-info.java` — add `<doclint>none</doclint>`.
2. **S17**: GPG loopback pinentry mode required in non-interactive CI — don't skip this config.
3. **S19**: Neo4j `MERGE` requires the constraint to exist first — schema initialiser (S20) must run before write tests in integration tests; for unit tests seed the constraint manually.
4. **S20**: `@AutoConfiguration(after = Neo4jDataAutoConfiguration.class)` prevents Neo4jClient being null at bean creation time.
5. **S21**: Filter `shouldNotFilter` check must be on request URI prefix, not `AntPathMatcher` — keep it simple.
6. **S22**: Spring Modulith 1.2.x and 1.3.x may have different internal class names for module detection — if the matrix fails, introduce `ModulithApiAdapter`.
7. **S24**: ClassGraph scans the full classpath if packages are empty — always guard against blank `base-packages` property.

---

## Success Criteria (Tier 3/4)

After all 9 stories complete:

- ✅ `io.eventus:eventus-spring:0.1.0` available on Maven Central
- ✅ GitHub Actions release workflow signs and publishes on `v*` tags
- ✅ Neo4j backend persists graph across restarts
- ✅ MCP endpoint protected by API key when enabled
- ✅ CI matrix verifies Modulith 1.2.x + 1.3.x compatibility
- ✅ Any JVM app can declare event topology with `@EventModule` / `@Publishes` / `@Listens`
- ✅ `mvn verify` green across all modules

---

## Files in This Tier

```
.claude/stories/
├── S16-maven-central-pom-metadata.md
├── S17-maven-central-release-workflow.md
├── S18-neo4j-module-scaffold.md
├── S19-neo4j-graph-writer-reader.md
├── S20-neo4j-autoconfiguration.md
├── S21-mcp-security.md
├── S22-version-compatibility-matrix.md
├── S23-generic-extractor-annotations.md
└── S24-generic-extractor-implementation.md
```
