package io.eventus.core.violations;

public enum ViolationType {
    CIRCULAR_EVENT_DEPENDENCY("Circular event chain detected"),
    HIDDEN_COUPLING("Module has listener relationship but no declared dependency"),
    UNUSED_EVENT("Event published but has no listeners"),
    FAILING_LISTENER("Listener consistently fails (>80% error rate)"),
    STALE_PUBLICATION("Event publication incomplete for extended period");

    private final String description;

    ViolationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
