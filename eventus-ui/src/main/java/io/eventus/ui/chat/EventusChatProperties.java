package io.eventus.ui.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eventus.chat")
public class EventusChatProperties {

    private boolean enabled = false;
    private String anthropicApiKey;
    private String model = "claude-sonnet-4-5";
    private int maxTokens = 1024;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getAnthropicApiKey() { return anthropicApiKey; }
    public void setAnthropicApiKey(String anthropicApiKey) { this.anthropicApiKey = anthropicApiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
}
