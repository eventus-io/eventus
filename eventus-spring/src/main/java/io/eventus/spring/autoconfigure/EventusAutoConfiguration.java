package io.eventus.spring.autoconfigure;

import io.eventus.core.GraphCacheAware;
import io.eventus.core.GraphReader;
import io.eventus.core.GraphWriter;
import io.eventus.core.drift.BaselineManager;
import io.eventus.core.drift.DriftAnalyzer;
import io.eventus.core.drift.InMemoryDriftAnalyzer;
import io.eventus.core.impact.ImpactAnalyzer;
import io.eventus.core.impact.InMemoryImpactAnalyzer;
import io.eventus.core.violations.InMemoryViolationAnalyzer;
import io.eventus.core.violations.ViolationAnalyzer;
import io.eventus.core.memory.InMemoryGraph;
import io.eventus.core.memory.InMemoryGraphReader;
import io.eventus.core.memory.InMemoryGraphWriter;
import io.eventus.spring.SpringModulithExtractor;
import io.eventus.spring.actuator.EventusEventsEndpoint;
import io.eventus.spring.actuator.EventusModulesEndpoint;
import io.eventus.spring.actuator.EventusPublicationsEndpoint;
import io.eventus.spring.drift.DriftController;
import io.eventus.spring.drift.FileSystemBaselineManager;
import io.eventus.spring.impact.ImpactAnalysisController;
import io.eventus.spring.violations.ViolationsController;
import io.eventus.spring.metrics.EventusMetricsCollector;
import io.eventus.spring.publications.ModulithPublicationBridge;
import io.eventus.spring.ui.EventusUIApiController;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.events.core.EventPublicationRepository;

@AutoConfiguration
@AutoConfigureAfter(name = "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration")
@ConditionalOnClass(ApplicationModules.class)
@ConditionalOnProperty(prefix = "eventus", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(EventusProperties.class)
public class EventusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InMemoryGraphWriter inMemoryGraphWriter() {
        return InMemoryGraph.writer();
    }

    @Bean
    @ConditionalOnMissingBean
    public InMemoryGraphReader inMemoryGraphReader(InMemoryGraphWriter writer) {
        return InMemoryGraph.reader(writer);
    }

    @Bean
    @ConditionalOnMissingBean(GraphWriter.class)
    public GraphWriter graphWriter(InMemoryGraphWriter writer) {
        return writer;
    }

    @Bean
    @ConditionalOnMissingBean(GraphReader.class)
    public GraphReader graphReader(InMemoryGraphReader reader) {
        return reader;
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringModulithExtractor springModulithExtractor(ApplicationContext ctx) {
        Class<?> mainClass = detectMainClass(ctx);
        return new SpringModulithExtractor(mainClass, ctx);
    }

    @Bean
    @ConditionalOnMissingBean(name = "eventusModulesEndpoint")
    public EventusModulesEndpoint eventusModulesEndpoint(GraphReader reader) {
        return new EventusModulesEndpoint(reader);
    }

    @Bean
    @ConditionalOnMissingBean(name = "eventusEventsEndpoint")
    public EventusEventsEndpoint eventusEventsEndpoint(GraphReader reader) {
        return new EventusEventsEndpoint(reader);
    }

    @Bean
    @ConditionalOnMissingBean(name = "eventusPublicationsEndpoint")
    public EventusPublicationsEndpoint eventusPublicationsEndpoint(GraphReader reader) {
        return new EventusPublicationsEndpoint(reader);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventusMetricsCollector eventusMetricsCollector(MeterRegistry meterRegistry, GraphReader reader) {
        return new EventusMetricsCollector(meterRegistry, reader);
    }

    @Bean
    @ConditionalOnMissingBean
    public ImpactAnalyzer impactAnalyzer(GraphReader reader, InMemoryGraphWriter writer) {
        InMemoryImpactAnalyzer analyzer = new InMemoryImpactAnalyzer(reader);
        writer.registerCacheAware(analyzer);
        return analyzer;
    }

    @Bean
    @ConditionalOnMissingBean
    public ImpactAnalysisController impactAnalysisController(ImpactAnalyzer analyzer) {
        return new ImpactAnalysisController(analyzer);
    }

    @Bean
    @ConditionalOnMissingBean
    public BaselineManager baselineManager() {
        java.nio.file.Path baselineDir = java.nio.file.Paths.get(System.getProperty("user.dir"), ".eventus");
        return new FileSystemBaselineManager(baselineDir);
    }

    @Bean
    @ConditionalOnMissingBean
    public DriftAnalyzer driftAnalyzer(GraphReader reader, BaselineManager baselineManager) {
        return new InMemoryDriftAnalyzer(reader, baselineManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public DriftController driftController(DriftAnalyzer driftAnalyzer, BaselineManager baselineManager, GraphReader reader) {
        return new DriftController(driftAnalyzer, baselineManager, reader);
    }

    @Bean
    @ConditionalOnMissingBean
    public ViolationAnalyzer violationAnalyzer(GraphReader reader, InMemoryGraphWriter writer) {
        InMemoryViolationAnalyzer analyzer = new InMemoryViolationAnalyzer(reader);
        writer.registerCacheAware(analyzer);
        return analyzer;
    }

    @Bean
    @ConditionalOnMissingBean
    public ViolationsController violationsController(ViolationAnalyzer analyzer) {
        return new ViolationsController(analyzer);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventusUIApiController eventusUIApiController(GraphReader reader,
            java.util.Optional<ModulithPublicationBridge> publicationBridge) {
        return new EventusUIApiController(reader, publicationBridge);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.modulith.events.core.EventPublicationRepository")
    @ConditionalOnBean(EventPublicationRepository.class)
    @ConditionalOnMissingBean
    public ModulithPublicationBridge modulithPublicationBridge(
            EventPublicationRepository repository, InMemoryGraphWriter writer) {
        return new ModulithPublicationBridge(repository, writer);
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> eventusExtractionTrigger(
            SpringModulithExtractor extractor, GraphWriter writer) {
        return event -> writer.write(extractor.extract());
    }

    private Class<?> detectMainClass(ApplicationContext ctx) {
        String[] names = ctx.getBeanNamesForAnnotation(
                org.springframework.boot.autoconfigure.SpringBootApplication.class);
        if (names.length > 0) {
            return org.springframework.aop.support.AopUtils.getTargetClass(ctx.getBean(names[0]));
        }
        return Object.class;
    }
}
