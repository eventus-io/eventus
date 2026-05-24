package io.eventus.core.violations;

import java.util.List;

public interface ViolationAnalyzer {
    List<Violation> analyze();
}
