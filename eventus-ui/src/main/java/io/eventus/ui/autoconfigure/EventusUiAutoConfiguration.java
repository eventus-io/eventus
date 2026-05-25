package io.eventus.ui.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "eventus.ui", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(EventusUiProperties.class)
public class EventusUiAutoConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // index.html must never be cached so re-deploys are picked up immediately
        registry.addResourceHandler("/eventus/index.html")
                .addResourceLocations("classpath:/static/eventus/")
                .setCachePeriod(0);
        // hashed asset bundles are safe to cache long-term
        registry.addResourceHandler("/eventus/**")
                .addResourceLocations("classpath:/static/eventus/")
                .setCachePeriod(31536000);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/eventus", "/eventus/index.html");
        registry.addRedirectViewController("/eventus/", "/eventus/index.html");
    }
}
