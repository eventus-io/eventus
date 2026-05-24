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
