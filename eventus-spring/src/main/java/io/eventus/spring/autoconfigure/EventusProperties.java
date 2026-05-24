package io.eventus.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "eventus")
public class EventusProperties {

    private boolean enabled = true;
    private Publications publications = new Publications();
    private UI ui = new UI();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Publications getPublications() { return publications; }
    public void setPublications(Publications publications) { this.publications = publications; }

    public UI getUi() { return ui; }
    public void setUi(UI ui) { this.ui = ui; }

    public static class Publications {
        private Duration staleThreshold = Duration.ofHours(2);

        public Duration getStaleThreshold() { return staleThreshold; }
        public void setStaleThreshold(Duration staleThreshold) { this.staleThreshold = staleThreshold; }
    }

    public static class UI {
        private boolean enabled = true;
        private Duration refreshInterval = Duration.ofSeconds(5);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public Duration getRefreshInterval() { return refreshInterval; }
        public void setRefreshInterval(Duration refreshInterval) { this.refreshInterval = refreshInterval; }
    }
}
