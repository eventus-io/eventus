# S06 — Spring Boot Auto-Configuration

## Goal
Wire everything together automatically. A Spring Modulith application adds
`eventus-spring` to its dependencies and gets the full toolchain with zero
manual configuration.

## Acceptance Criteria
- [ ] `EventusAutoConfiguration` is registered in
      `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- [ ] Auto-configuration creates `InMemoryGraphWriter` bean
- [ ] Auto-configuration creates `InMemoryGraphReader` bean (sharing the same store)
- [ ] Auto-configuration creates `SpringModulithExtractor` bean
      (conditional on `ApplicationModules` class being present)
- [ ] Auto-configuration creates all three actuator endpoint beans
- [ ] Auto-configuration runs extraction on `ApplicationReadyEvent`
      (after all beans are initialised)
- [ ] Auto-configuration is `@ConditionalOnClass(ApplicationModules.class)`
      so it does nothing if Spring Modulith is not on the classpath
- [ ] All beans are `@ConditionalOnMissingBean` so users can override any of them
- [ ] Configuration property `eventus.enabled=true` (default) allows full opt-out
- [ ] Configuration property `eventus.publications.stale-threshold=PT2H` (default)

## Auto-Configuration Class

```java
package io.eventus.spring.autoconfigure;

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.modulith.core.ApplicationModules")
@ConditionalOnProperty(prefix = "eventus", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(EventusProperties.class)
public class EventusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InMemoryGraphWriter inMemoryGraphWriter() { ... }

    @Bean
    @ConditionalOnMissingBean
    public InMemoryGraphReader inMemoryGraphReader(InMemoryGraphWriter writer) { ... }

    @Bean
    @ConditionalOnMissingBean
    public SpringModulithExtractor springModulithExtractor(
        ApplicationContext ctx,
        ObjectProvider<EventPublicationRepository> publicationRepository,
        EventusProperties properties
    ) {
        // resolve main application class from SpringApplication.sources
        // or fallback to scanning for @SpringBootApplication
    }

    @Bean
    @ConditionalOnMissingBean
    public EventusModulesEndpoint eventusModulesEndpoint(InMemoryGraphReader reader) { ... }

    @Bean
    @ConditionalOnMissingBean
    public EventusEventsEndpoint eventusEventsEndpoint(InMemoryGraphReader reader) { ... }

    @Bean
    @ConditionalOnMissingBean
    public EventusPublicationsEndpoint eventusPublicationsEndpoint(InMemoryGraphReader reader) { ... }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> eventusExtractionTrigger(
        SpringModulithExtractor extractor,
        InMemoryGraphWriter writer
    ) {
        return event -> {
            GraphModel model = extractor.extract();
            writer.write(model);
        };
    }
}
```

## EventusProperties

```java
package io.eventus.spring.autoconfigure;

@ConfigurationProperties(prefix = "eventus")
public class EventusProperties {
    private boolean enabled = true;
    private Publications publications = new Publications();

    public static class Publications {
        private Duration staleThreshold = Duration.ofHours(2);
        // getters/setters
    }
    // getters/setters
}
```

## Registration File

Create:
```
eventus-spring/src/main/resources/META-INF/spring/
  org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Contents:
```
io.eventus.spring.autoconfigure.EventusAutoConfiguration
```

## Additional Configuration

Create `eventus-spring/src/main/resources/META-INF/spring-configuration-metadata.json`
with property descriptions for IDE autocompletion:
```json
{
  "properties": [
    {
      "name": "eventus.enabled",
      "type": "java.lang.Boolean",
      "description": "Enable Eventus graph extraction. Default: true.",
      "defaultValue": true
    },
    {
      "name": "eventus.publications.stale-threshold",
      "type": "java.time.Duration",
      "description": "Duration after which an incomplete publication is considered stale. Default: PT2H.",
      "defaultValue": "PT2H"
    }
  ]
}
```

## Tests Required

### EventusAutoConfigurationTest
Use `ApplicationContextRunner` (Spring Boot test utility):

```java
@Test
void autoConfigurationCreatesAllBeans() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(EventusAutoConfiguration.class))
        .run(ctx -> {
            assertThat(ctx).hasSingleBean(InMemoryGraphWriter.class);
            assertThat(ctx).hasSingleBean(InMemoryGraphReader.class);
            assertThat(ctx).hasSingleBean(EventusModulesEndpoint.class);
            assertThat(ctx).hasSingleBean(EventusEventsEndpoint.class);
            assertThat(ctx).hasSingleBean(EventusPublicationsEndpoint.class);
        });
}

@Test
void autoConfigurationBacksOffWhenDisabled() {
    new ApplicationContextRunner()
        .withPropertyValues("eventus.enabled=false")
        .withConfiguration(AutoConfigurations.of(EventusAutoConfiguration.class))
        .run(ctx -> {
            assertThat(ctx).doesNotHaveBean(EventusModulesEndpoint.class);
        });
}
```

## Done When
`mvn test -pl eventus-spring` passes including auto-configuration tests.
A minimal Spring Modulith app with only `eventus-spring` on the classpath
starts up and exposes the three actuator endpoints automatically.
