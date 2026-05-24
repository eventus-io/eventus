package io.eventus.core.impact;

public interface ImpactAnalyzer {
    EventImpactResponse analyzeEventImpact(String eventId);
    ModuleImpactResponse analyzeModuleImpact(String moduleId);
}
