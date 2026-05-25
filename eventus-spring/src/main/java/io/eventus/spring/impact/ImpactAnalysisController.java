package io.eventus.spring.impact;

import io.eventus.core.impact.EventImpactResponse;
import io.eventus.core.impact.EventNotFoundException;
import io.eventus.core.impact.ImpactAnalyzer;
import io.eventus.core.impact.ModuleImpactResponse;
import io.eventus.core.impact.ModuleNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eventus/api/impact")
public class ImpactAnalysisController {

    private final ImpactAnalyzer impactAnalyzer;

    public ImpactAnalysisController(ImpactAnalyzer impactAnalyzer) {
        this.impactAnalyzer = impactAnalyzer;
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<EventImpactResponse> analyzeEvent(@PathVariable("eventId") String eventId) {
        try {
            return ResponseEntity.ok(impactAnalyzer.analyzeEventImpact(eventId));
        } catch (EventNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/module/{moduleId}")
    public ResponseEntity<ModuleImpactResponse> analyzeModule(@PathVariable("moduleId") String moduleId) {
        try {
            return ResponseEntity.ok(impactAnalyzer.analyzeModuleImpact(moduleId));
        } catch (ModuleNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
