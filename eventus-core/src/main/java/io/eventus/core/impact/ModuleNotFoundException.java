package io.eventus.core.impact;

public class ModuleNotFoundException extends RuntimeException {
    public ModuleNotFoundException(String moduleId) {
        super("Module not found: " + moduleId);
    }
}
