# Eventus Tier 2 — Extended (v0.2–0.4) Story Summary

**Status**: Tier 1 (v0.1) ✅ Complete  
**Current**: Tier 2 (v0.2–0.4) Stories Ready for Delegation

---

## Overview

Tier 2 extends Eventus from a JSON-only API into a fully observability platform with three main areas of focus:

1. **Visualization & Usability** (v0.2) — React UI for exploring topologies
2. **Governance & Safety** (v0.3–0.4) — Impact analysis, violation detection, architectural drift
3. **Cross-Service Support** (v0.3) — Kafka / Spring Cloud Stream integration

All stories are designed to be independently delegatable to Claude Code CLI.

---

## Story Roadmap

| Story | Version | Title | Focus | Dependencies |
|-------|---------|-------|-------|--------------|
| S10 | v0.2 | Embedded React UI | Interactive graph visualization | eventus-spring v0.1 |
| S11 | v0.3 | Impact Analysis API | Graph traversal queries | eventus-core, eventus-spring |
| S12 | v0.3 | Grafana & Metrics | Observability metrics + dashboard | eventus-spring, Micrometer |
| S13 | v0.4 | Violation Detection | Architectural rule enforcement | eventus-core, eventus-spring |
| S14 | v0.3 | Spring Cloud Streams Extractor | Kafka topology extraction | **new module**: eventus-streams |
| S15 | v0.4 | Architectural Drift Detection | Baseline comparison | eventus-core, eventus-spring |

---

## Key Design Decisions (Tier 2)

1. **UI-first for adoption**: React dashboard (S10) is v0.2, not v1.0. Drives early adoption.
2. **Observability integrations**: Grafana (S12) opens platform engineering audience.
3. **Pluggable extractors**: Spring Cloud Streams (S14) proves the `EventGraphExtractor` interface scales.
4. **Governance without mandate**: Violations (S13) and drift (S15) inform without enforcing.
5. **Independent modules**: Each story can be completed separately; stories don't block one another.

---

## Execution Checklist

For each story:

- [ ] Read story file completely (acceptance criteria + design + tests)
- [ ] Understand dependencies (what modules it touches)
- [ ] Run tests: `mvn test -pl <module>` should pass at the end
- [ ] Verify no breaking changes to prior stories
- [ ] Update README with new feature

### Before Starting a Story

1. Ensure v0.1 tests still pass: `mvn verify`
2. Create a feature branch: `git checkout -b <story-id>`
3. Use story ID in commit messages: `feat(S10): embedded React UI`

### After Completing a Story

1. All tests green: `mvn verify`
2. No IDE warnings or security issues flagged
3. README updated with new feature + usage example
4. Commit and push: `git push origin <story-id>`

---

## Story Difficulty & Estimated Time

| Story | Difficulty | Est. Time | Notes |
|-------|------------|-----------|-------|
| S10 | Medium | 6–8 hours | Frontend + REST controller + tests; React Flow learning curve |
| S11 | Medium | 4–6 hours | Graph traversal; similar to S10 complexity |
| S12 | Easy | 3–4 hours | Mostly configuration; Grafana dashboard is straightforward JSON |
| S13 | Hard | 8–10 hours | Circular dependency detection is non-trivial; multiple algorithms |
| S14 | Medium | 5–7 hours | New module; embedded Kafka testing required |
| S15 | Medium | 6–8 hours | File I/O + drift comparison logic; baseline versioning |

---

## Testing Strategy (Tier 2)

### Unit Tests
- Per-story tests in `eventus-core/src/test` and `eventus-spring/src/test`
- Mock `GraphReader` for most tests
- Use real `InMemoryGraphWriter` to build test graphs

### Integration Tests
- Use `@SpringBootTest` with test application from S04
- For S14 (Kafka), use `@EmbeddedKafka`
- All tests must pass: `mvn verify`

### Manual Verification
- S10: Open browser to `http://localhost:8080` and click around
- S12: Import Grafana dashboard JSON, verify panels load
- S13: Add violations to test data, verify REST response
- S15: Save baseline, add/remove a module, verify drift detection

---

## Module Dependencies

```
eventus-core (no Spring)
├── [S11] ImpactAnalyzer
├── [S13] ViolationAnalyzer
└── [S15] DriftAnalyzer, BaselineManager

eventus-spring (Spring Modulith)
├── [S10] EventusUIApiController, EventusUIRouter
├── [S11] ImpactAnalysisController (wraps core)
├── [S12] EventusMetricsCollector
├── [S13] ViolationsController (wraps core)
└── [S15] DriftController (wraps core)

eventus-streams (new, Spring Cloud Stream)
└── [S14] SpringCloudStreamExtractor
```

**Auto-configuration** registers all new beans automatically.

---

## Git Workflow

```bash
# Start a story
git checkout -b S10-embedded-react-ui

# Commit as you go
git commit -m "feat(S10): add React Flow component"
git commit -m "test(S10): add UI controller tests"

# Before merging back to main
mvn verify
git push origin S10-embedded-react-ui

# (Then create PR, get review, merge)
git checkout main && git pull
```

---

## Common Pitfalls

1. **S10**: React state management — use `useState` not `localStorage` (not available)
2. **S11**: Graph traversal can be O(n²) — cache results if > 100 modules
3. **S12**: Grafana JSON must use valid panel types — validate in Grafana first
4. **S13**: Circular dependency detection is CPU-intensive — test with >100 modules
5. **S14**: Kafka binding configuration varies by binder — handle gracefully
6. **S15**: Baseline JSON can grow large — consider compression in v0.5

---

## Success Criteria (Tier 2)

After all 6 stories complete:

- ✅ Dashboard renders module graph with colors + click interaction
- ✅ Impact analysis API answers "what breaks if I change this?"
- ✅ Grafana dashboard shows health and event flow
- ✅ Violations endpoint detects circular deps, unused events, failing listeners
- ✅ Spring Cloud Stream applications can be visualized alongside Spring Modulith
- ✅ Drift detection catches architectural changes vs. baseline
- ✅ All tests green: `mvn verify`
- ✅ README updated with v0.2–0.4 features + examples
- ✅ GitHub Actions CI passes on every commit

---

## Next: Tier 3 (v1.0)

Once Tier 2 is complete, Tier 3 focuses on the **AI layer**:

- **S16**: MCP server exposing graph as callable LLM tools
- **S17**: Natural language query panel in React UI
- **S18**: AI architectural guidance (LLM analyzes coupling, suggests refactors)
- **S19**: GitHub Actions bot that runs drift detection on every PR

Tier 3 is scheduled for Month 5–6 and targets the **AI-assisted development** audience.

---

## How to Delegate to Claude Code

Each story is a standalone `.md` file ready for Claude Code:

```bash
# In your IDE (VS Code + Claude Code extension)
/code S10-embedded-react-ui.md

# Or via CLI
claude code S10-embedded-react-ui.md
```

Claude Code will:
1. Read the story (goal, acceptance criteria, implementation)
2. Create the code changes
3. Run tests
4. Commit with conventional message

All stories follow the same format for consistency.

---

## Files in This Tier

```
.claude/stories/
├── S10-embedded-react-ui.md
├── S11-impact-analysis-api.md
├── S12-grafana-and-metrics.md
├── S13-violation-detection.md
├── S14-spring-cloud-streams-extractor.md
└── S15-architectural-drift-detection.md
```

Copy these files to `.claude/stories/` in your Eventus repo, then delegate via Claude Code.

---

## Questions?

Refer back to the **Eventus — Complete Project Reference** document for:
- Architecture overview
- Module structure
- Coding conventions
- Release timeline
- OSS strategy
