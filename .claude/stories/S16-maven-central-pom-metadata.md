# S16 — Maven Central: POM Metadata + Source/Javadoc Plugins

## Goal
Add all metadata and plugin configuration required by Maven Central to the root POM and every module POM so artifacts can be published to OSSRH.

## Acceptance Criteria
- [ ] Root POM has `<name>`, `<description>`, `<url>`, `<licenses>`, `<developers>`, `<scm>` blocks
- [ ] All module POMs inherit or override `<name>` and `<description>`
- [ ] `maven-source-plugin` attaches `-sources.jar` on `package`
- [ ] `maven-javadoc-plugin` attaches `-javadoc.jar` on `package`
- [ ] GPG signing plugin configured under a `release` profile (not active by default)
- [ ] `mvn package` succeeds locally without the `release` profile
- [ ] `mvn package -Prelease` succeeds (skipping actual GPG signing when key absent via `gpg.skip=true`)
- [ ] All existing tests still green: `mvn verify`

## POM Changes Required

### Root `pom.xml` additions

```xml
<name>Eventus</name>
<description>Event topology, made visible. Extracts module and event graphs from running JVM applications.</description>
<url>https://github.com/rafaelmaia/eventus</url>

<licenses>
  <license>
    <name>Apache License, Version 2.0</name>
    <url>https://www.apache.org/licenses/LICENSE-2.0</url>
    <distribution>repo</distribution>
  </license>
</licenses>

<developers>
  <developer>
    <id>rafaelmaia</id>
    <name>Rafael Maia</name>
    <email>xrafaoliveira@gmail.com</email>
    <url>https://github.com/rafaelmaia</url>
  </developer>
</developers>

<scm>
  <connection>scm:git:git://github.com/rafaelmaia/eventus.git</connection>
  <developerConnection>scm:git:ssh://github.com/rafaelmaia/eventus.git</developerConnection>
  <url>https://github.com/rafaelmaia/eventus/tree/main</url>
  <tag>HEAD</tag>
</scm>
```

### Source and Javadoc plugins (in root `<build><plugins>`)

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-source-plugin</artifactId>
  <version>3.3.1</version>
  <executions>
    <execution>
      <id>attach-sources</id>
      <goals><goal>jar-no-fork</goal></goals>
    </execution>
  </executions>
</plugin>

<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-javadoc-plugin</artifactId>
  <version>3.6.3</version>
  <configuration>
    <doclint>none</doclint>
  </configuration>
  <executions>
    <execution>
      <id>attach-javadocs</id>
      <goals><goal>jar</goal></goals>
    </execution>
  </executions>
</plugin>
```

### Release profile (GPG signing — not active by default)

```xml
<profiles>
  <profile>
    <id>release</id>
    <build>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-gpg-plugin</artifactId>
          <version>3.2.4</version>
          <executions>
            <execution>
              <id>sign-artifacts</id>
              <phase>verify</phase>
              <goals><goal>sign</goal></goals>
              <configuration>
                <gpgArguments>
                  <arg>--pinentry-mode</arg>
                  <arg>loopback</arg>
                </gpgArguments>
              </configuration>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

### Distribution management (OSSRH)

```xml
<distributionManagement>
  <snapshotRepository>
    <id>ossrh</id>
    <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
  </snapshotRepository>
  <repository>
    <id>ossrh</id>
    <url>https://s01.oss.sonatype.org/service/local/staging/deploy/maven2</url>
  </repository>
</distributionManagement>
```

### Module POM names

Each module POM needs a `<name>` element:

| Module | `<name>` |
|---|---|
| `eventus-core` | Eventus Core |
| `eventus-spring` | Eventus Spring |
| `eventus-mcp` | Eventus MCP |
| `eventus-streams` | Eventus Streams |
| `eventus-generic` | Eventus Generic |
| `eventus-ui` | Eventus UI |

## Done When
- `mvn package` succeeds without errors and produces `-sources.jar` and `-javadoc.jar` for each module
- `mvn package -Prelease -Dgpg.skip=true` succeeds
- `mvn verify` still passes all tests
- All required Maven Central metadata fields are present in the root POM
