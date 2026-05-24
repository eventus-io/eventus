# S18 — Neo4j Backend: Module Scaffold + Connection Configuration

## Goal
Create the `eventus-neo4j` module with its Maven structure, dependency declarations, and auto-configuration skeleton. No logic yet — just the scaffold that S19 and S20 will build on.

## Acceptance Criteria
- [ ] `eventus-neo4j/` module exists and is declared in root `pom.xml`
- [ ] Module POM depends on `eventus-core`, `spring-data-neo4j`, and `spring-boot-autoconfigure`
- [ ] Package `io.eventus.neo4j` and `io.eventus.neo4j.autoconfigure` exist
- [ ] `EventusNeo4jAutoConfiguration` stub exists (no beans yet, just `@Configuration` + `@ConditionalOnClass(Neo4jTemplate.class)`)
- [ ] `spring.factories` / `AutoConfiguration.imports` registers the auto-configuration class
- [ ] Module compiles: `mvn compile -pl eventus-neo4j` passes
- [ ] No other modules are broken: `mvn verify` passes

## Module Structure

```
eventus-neo4j/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── io/eventus/neo4j/
    │   │       ├── Neo4jGraphWriter.java       (stub — implements GraphWriter, all methods throw UnsupportedOperationException)
    │   │       ├── Neo4jGraphReader.java       (stub — implements GraphReader, all methods throw UnsupportedOperationException)
    │   │       └── autoconfigure/
    │   │           └── EventusNeo4jAutoConfiguration.java
    │   └── resources/
    │       └── META-INF/spring/
    │           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── test/
        └── java/
            └── io/eventus/neo4j/
                └── Neo4jModuleCompilationTest.java
```

## Module POM

```xml
<project>
  <parent>
    <groupId>io.eventus</groupId>
    <artifactId>eventus-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>

  <artifactId>eventus-neo4j</artifactId>
  <name>Eventus Neo4j</name>
  <description>Neo4j graph backend for Eventus — production-grade persistence across restarts</description>

  <dependencies>
    <dependency>
      <groupId>io.eventus</groupId>
      <artifactId>eventus-core</artifactId>
      <version>${project.version}</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-autoconfigure</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-neo4j</artifactId>
      <optional>true</optional>
    </dependency>

    <!-- Test -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-testcontainers</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>neo4j</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

## Auto-Configuration Stub

```java
// io.eventus.neo4j.autoconfigure/EventusNeo4jAutoConfiguration.java
@Configuration
@ConditionalOnClass(name = "org.springframework.data.neo4j.core.Neo4jTemplate")
@ConditionalOnProperty(prefix = "eventus.neo4j", name = "enabled", matchIfMissing = true)
@AutoConfiguration
public class EventusNeo4jAutoConfiguration {
    // Beans added in S20 after Neo4jGraphWriter/Reader are implemented in S19
}
```

## AutoConfiguration.imports

```
io.eventus.neo4j.autoconfigure.EventusNeo4jAutoConfiguration
```

## Compilation Test

```java
// Verifies the module compiles and the auto-config class is loadable
class Neo4jModuleCompilationTest {
    @Test
    void autoConfigurationClassIsLoadable() {
        assertThatNoException().isThrownBy(() ->
            Class.forName("io.eventus.neo4j.autoconfigure.EventusNeo4jAutoConfiguration")
        );
    }
}
```

## Done When
- `mvn compile -pl eventus-neo4j` passes
- `mvn verify` passes (all modules)
- Module appears in root POM `<modules>` list
- Auto-configuration registered in `AutoConfiguration.imports`
