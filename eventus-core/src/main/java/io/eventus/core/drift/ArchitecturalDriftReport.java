package io.eventus.core.drift;

import java.util.List;

public record ArchitecturalDriftReport(
        List<Drift> drifts,
        int totalDrifts,
        int breachingCount,
        long comparedAt
) {}
