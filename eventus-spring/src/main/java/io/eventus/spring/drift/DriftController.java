package io.eventus.spring.drift;

import io.eventus.core.GraphReader;
import io.eventus.core.drift.ArchitecturalDriftReport;
import io.eventus.core.drift.BaselineManager;
import io.eventus.core.drift.BaselineSnapshot;
import io.eventus.core.drift.DriftAnalyzer;
import io.eventus.core.model.GraphModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/eventus/api/drift")
public class DriftController {

    private final DriftAnalyzer driftAnalyzer;
    private final BaselineManager baselineManager;
    private final GraphReader graphReader;

    public DriftController(DriftAnalyzer driftAnalyzer, BaselineManager baselineManager, GraphReader graphReader) {
        this.driftAnalyzer = driftAnalyzer;
        this.baselineManager = baselineManager;
        this.graphReader = graphReader;
    }

    @GetMapping
    public ArchitecturalDriftReport getDrift() {
        return driftAnalyzer.analyzeDrift();
    }

    @PostMapping("/baseline")
    public ResponseEntity<Void> captureBaseline() {
        var model = new GraphModel();
        graphReader.getModules().forEach(model::addModule);
        graphReader.getEvents().forEach(model::addEvent);
        graphReader.getEdges().forEach(model::addEdge);
        baselineManager.saveBaseline(model);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/baseline")
    public ResponseEntity<BaselineSnapshot> getBaseline() {
        BaselineSnapshot baseline = baselineManager.loadBaseline();
        if (baseline == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(baseline);
    }
}
