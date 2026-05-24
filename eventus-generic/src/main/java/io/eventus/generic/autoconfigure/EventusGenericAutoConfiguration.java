package io.eventus.generic.autoconfigure;

import io.eventus.core.EventGraphExtractor;
import io.eventus.generic.AnnotationBasedExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.List;

@AutoConfiguration
@ConditionalOnProperty(prefix = "eventus.generic", name = "base-packages")
public class EventusGenericAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventGraphExtractor.class)
    public EventGraphExtractor annotationBasedExtractor(
            @Value("${eventus.generic.base-packages}") String basePackages) {
        List<String> packages = Arrays.stream(basePackages.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        return new AnnotationBasedExtractor(packages);
    }
}
