package io.eventus.spring.drift;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.eventus.core.drift.BaselineManager;
import io.eventus.core.drift.BaselineSnapshot;
import io.eventus.core.model.GraphModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemBaselineManager implements BaselineManager {

    private final Path baselinePath;
    private final ObjectMapper mapper;

    public FileSystemBaselineManager(Path baselineDir) {
        this.baselinePath = baselineDir.resolve("baseline.json");
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    @Override
    public BaselineSnapshot loadBaseline() {
        if (!Files.exists(baselinePath)) {
            return null;
        }
        try {
            return mapper.readValue(baselinePath.toFile(), BaselineSnapshot.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load baseline: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveBaseline(GraphModel current) {
        try {
            Files.createDirectories(baselinePath.getParent());
            var snapshot = new BaselineSnapshot(
                    current.modules(),
                    current.events(),
                    current.edges(),
                    System.currentTimeMillis()
            );
            mapper.writerWithDefaultPrettyPrinter().writeValue(baselinePath.toFile(), snapshot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save baseline: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasBaseline() {
        return Files.exists(baselinePath);
    }
}
