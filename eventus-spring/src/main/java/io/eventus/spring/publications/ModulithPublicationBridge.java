package io.eventus.spring.publications;

import io.eventus.core.memory.InMemoryGraphWriter;
import io.eventus.core.model.PublicationRecord;
import io.eventus.core.model.PublicationStatus;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.core.EventPublicationRepository;
import org.springframework.modulith.events.core.TargetEventPublication;

import java.util.List;

public class ModulithPublicationBridge {

    private final EventPublicationRepository repository;
    private final InMemoryGraphWriter writer;

    public ModulithPublicationBridge(EventPublicationRepository repository, InMemoryGraphWriter writer) {
        this.repository = repository;
        this.writer = writer;
    }

    public void sync() {
        List<PublicationRecord> records = repository.findByStatus(EventPublication.Status.COMPLETED).stream()
                .map(p -> toRecord(p, PublicationStatus.COMPLETED))
                .toList();

        List<PublicationRecord> incomplete = repository.findIncompletePublications().stream()
                .map(p -> toRecord(p, PublicationStatus.INCOMPLETE))
                .toList();

        List<PublicationRecord> failed = repository.findByStatus(EventPublication.Status.FAILED).stream()
                .map(p -> toRecord(p, PublicationStatus.STALE))
                .toList();

        List<PublicationRecord> all = new java.util.ArrayList<>();
        all.addAll(records);
        all.addAll(incomplete);
        all.addAll(failed);
        all.sort(java.util.Comparator.comparing(PublicationRecord::publishedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));

        writer.updatePublications(all);
    }

    private PublicationRecord toRecord(TargetEventPublication p, PublicationStatus status) {
        String listenerName = p.getTargetIdentifier().getValue();
        String eventType = p.getEvent().getClass().getName();
        return new PublicationRecord(
                p.getIdentifier().toString(),
                eventType,
                listenerName,
                null,
                status,
                p.getPublicationDate()
        );
    }
}
