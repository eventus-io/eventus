package io.eventus.examples.modulith.recommendations;

import io.eventus.examples.modulith.catalog.BookPublished;
import io.eventus.examples.modulith.catalog.PriceChanged;
import io.eventus.examples.modulith.reviews.ReviewPosted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Maintains in-memory recommendation scores per ISBN.
 * Ce = 3 (catalog × 2, reviews × 1) / Ca = 0 → I = 1.0 (fully unstable).
 * RecommendationUpdated has no consumers — UNUSED_EVENT violation.
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final ApplicationEventPublisher events;
    private final Map<String, Double> scores = new HashMap<>();

    public RecommendationService(ApplicationEventPublisher events) {
        this.events = events;
    }

    @ApplicationModuleListener
    void onReviewPosted(ReviewPosted e) {
        double updated = scores.getOrDefault(e.isbn(), 3.0) * 0.9 + e.rating() * 0.1;
        scores.put(e.isbn(), updated);
        events.publishEvent(new RecommendationUpdated(e.isbn(), updated));
        log.debug("[RECOMMENDATIONS] Score {} → {}", e.isbn(), updated);
    }

    @ApplicationModuleListener
    void onPriceChanged(PriceChanged e) {
        if (e.newPrice() < e.oldPrice()) {
            double boosted = scores.getOrDefault(e.isbn(), 3.0) * 1.05;
            scores.put(e.isbn(), boosted);
            events.publishEvent(new RecommendationUpdated(e.isbn(), boosted));
        }
    }

    @ApplicationModuleListener
    void onBookPublished(BookPublished e) {
        scores.put(e.isbn(), 3.0);
        events.publishEvent(new RecommendationUpdated(e.isbn(), 3.0));
        log.info("[RECOMMENDATIONS] Seeded score for new book {}", e.isbn());
    }

    public Map<String, Double> getScores() { return Map.copyOf(scores); }
}
