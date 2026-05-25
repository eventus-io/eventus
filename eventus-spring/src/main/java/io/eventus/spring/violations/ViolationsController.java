package io.eventus.spring.violations;

import io.eventus.core.violations.Violation;
import io.eventus.core.violations.ViolationAnalyzer;
import io.eventus.core.violations.ViolationSeverity;
import io.eventus.core.violations.ViolationType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/eventus/api/violations")
public class ViolationsController {

    private final ViolationAnalyzer violationAnalyzer;

    public ViolationsController(ViolationAnalyzer violationAnalyzer) {
        this.violationAnalyzer = violationAnalyzer;
    }

    @GetMapping
    public List<Violation> getViolations(
            @RequestParam(name = "severity", required = false) ViolationSeverity severity,
            @RequestParam(name = "type", required = false) ViolationType type) {
        return violationAnalyzer.analyze().stream()
                .filter(v -> severity == null || v.severity() == severity)
                .filter(v -> type == null || v.type() == type)
                .toList();
    }
}
