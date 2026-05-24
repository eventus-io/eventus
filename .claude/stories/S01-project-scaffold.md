# S01 — Project Scaffold

## Goal
Create the Maven multi-module project skeleton that all subsequent stories build on.

## Acceptance Criteria
- [ ] Root `pom.xml` exists as a parent POM with `<packaging>pom</packaging>`
- [ ] Three child modules declared: `eventus-core`, `eventus-spring`, `eventus-generic`
- [ ] Each module has its own `pom.xml` with correct `<parent>` reference
- [ ] `mvn clean install -DskipTests` succeeds from the root directory
- [ ] `.gitignore` covers Java/Maven artifacts (`target/`, `*.class`, `.idea/`, etc.)
- [ ] `LICENSE` file contains Apache 2.0 full text
- [ ] Directory structure matches the layout below

## Directory Layout

```
eventus/
├── pom.xml                          ← parent POM
├── LICENSE
├── .gitignore
├── CLAUDE.md
├── .claude/
│   └── stories/
├── eventus-core/
│   ├── pom.xml
│   └── src/
│       ├── main/java/io/eventus/core/
│       └── test/java/io/eventus/core/
├── eventus-spring/
│   ├── pom.xml
│   └── src/
│       ├── main/java/io/eventus/spring/
│       └── test/java/io/eventus/spring/
└── eventus-generic/
    ├── pom.xml
    └── src/
        ├── main/java/io/eventus/generic/
        └── test/java/io/eventus/generic/
```

## Root pom.xml Requirements

```xml
<groupId>io.eventus</groupId>
<artifactId>eventus</artifactId>
<version>0.1.0-SNAPSHOT</version>
<packaging>pom</packaging>

<properties>
  <java.version>21</java.version>
  <spring-boot.version>3.3.x</spring-boot.version>
  <spring-modulith.version>1.3.x</spring-modulith.version>
  <maven.compiler.source>21</maven.compiler.source>
  <maven.compiler.target>21</maven.compiler.target>
</properties>
```

- Import `spring-boot-dependencies` BOM in `<dependencyManagement>`
- Import `spring-modulith-bom` in `<dependencyManagement>`
- Add `maven-surefire-plugin` configured for JUnit 5

## Module pom.xml Requirements

### eventus-core
- No Spring dependencies
- Dependencies: none (pure Java)
- Test dependencies: `junit-jupiter`, `assertj-core`, `mockito-core`

### eventus-spring
- Depends on `eventus-core`
- Dependencies: `spring-boot-starter-actuator`, `spring-modulith-core` (optional)
- Test dependencies: `spring-boot-starter-test`, `spring-modulith-test`

### eventus-generic
- Depends on `eventus-core`
- Dependencies: none beyond core
- Test dependencies: `junit-jupiter`, `assertj-core`

## Implementation Notes
- Use `<relativePath>../pom.xml</relativePath>` in each child module parent ref
- Do not add Spring Boot plugin to root — only where needed
- Placeholder `package-info.java` files are fine to make directories non-empty

## Done When
`mvn clean install -DskipTests` from root prints `BUILD SUCCESS` with all 3 modules.
