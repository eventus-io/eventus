# S22 — Spring Modulith Version Compatibility Matrix

## Goal
Add a GitHub Actions matrix CI job that verifies `eventus-spring` works across multiple Spring Modulith versions, and document the compatibility table in the README.

## Acceptance Criteria
- [ ] `.github/workflows/compatibility.yml` runs on push to `main` and on PRs
- [ ] Matrix tests Spring Modulith versions: `1.2.x`, `1.3.x`
- [ ] Each matrix cell runs `mvn verify -pl eventus-spring,eventus-core`
- [ ] Failures in one matrix cell do not cancel the others (`fail-fast: false`)
- [ ] Compatibility table added to README
- [ ] If Modulith's internal APIs differ between versions, an `EventGraphExtractorAdapter` interface isolates version-specific calls
- [ ] All existing tests still green: `mvn verify`

## Workflow File

```yaml
# .github/workflows/compatibility.yml
name: Spring Modulith Compatibility

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  compatibility:
    name: Modulith ${{ matrix.modulith-version }}
    runs-on: ubuntu-latest
    fail-fast: false

    strategy:
      matrix:
        modulith-version: ["1.2.6", "1.3.3"]
        # Add 2.0.x here once it reaches GA

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Verify eventus-spring against Modulith ${{ matrix.modulith-version }}
        run: |
          mvn verify -pl eventus-core,eventus-spring \
            -am \
            -Dspring-modulith.version=${{ matrix.modulith-version }} \
            -Dsurefire.failIfNoSpecifiedTests=false
```

## Root POM Version Property

The root `pom.xml` must already declare `spring-modulith.version` as a property (so the matrix can override it):

```xml
<properties>
  <spring-modulith.version>1.3.3</spring-modulith.version>
</properties>
```

And in the dependency management for Spring Modulith:

```xml
<dependency>
  <groupId>org.springframework.modulith</groupId>
  <artifactId>spring-modulith-bom</artifactId>
  <version>${spring-modulith.version}</version>
  <type>pom</type>
  <scope>import</scope>
</dependency>
```

## API Adapter (if needed)

If the Spring Modulith API for module discovery differs between versions, introduce:

```java
// io.eventus.spring.internal/ModulithApiAdapter.java
interface ModulithApiAdapter {
    List<ModuleNode> extractModules(ApplicationContext context);
}

// io.eventus.spring.internal/Modulith13Adapter.java  (default)
class Modulith13Adapter implements ModulithApiAdapter {
    // Uses 1.3.x API
}
```

Register the correct adapter via `@ConditionalOnClass` checks for version-specific classes.
Only introduce this if the CI matrix actually reveals incompatibilities.

## README Section to Add

```markdown
## Compatibility

| Eventus | Spring Modulith | Spring Boot |
|---------|-----------------|-------------|
| 0.1.x   | 1.2.x, 1.3.x    | 3.2.x, 3.3.x |
| 1.0.x   | 1.3.x, 2.0.x    | 3.3.x, 3.4.x |

The compatibility matrix is verified automatically on every commit via GitHub Actions.
```

## Badge to Add to README

```markdown
[![Modulith Compatibility](https://github.com/rafaelmaia/eventus/actions/workflows/compatibility.yml/badge.svg)](https://github.com/rafaelmaia/eventus/actions/workflows/compatibility.yml)
```

## Done When
- `.github/workflows/compatibility.yml` exists and the matrix runs on push
- Both Modulith versions pass in CI
- `spring-modulith.version` is a root POM property
- Compatibility table + badge in README
- `mvn verify` still passes locally (default version unchanged)
