package io.eventus.examples.modulith.reviews;

import io.eventus.examples.modulith.fulfillment.ShipmentDelivered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviews;
    private final ApplicationEventPublisher events;

    public ReviewService(ReviewRepository reviews, ApplicationEventPublisher events) {
        this.reviews = reviews;
        this.events = events;
    }

    @ApplicationModuleListener
    void onShipmentDelivered(ShipmentDelivered e) {
        log.info("[REVIEWS] Order {} delivered — customer may now post a review", e.orderId());
    }

    public Review post(String isbn, String customerId, int rating, String comment) {
        var review = new Review(UUID.randomUUID().toString(), isbn, customerId, rating, comment);
        reviews.save(review);
        events.publishEvent(new ReviewPosted(review.getId(), isbn, customerId, rating));
        log.info("[REVIEWS] Review posted for {} by {} — {} stars", isbn, customerId, rating);
        return review;
    }

    public void flag(String reviewId, String reason) {
        reviews.findById(reviewId).ifPresent(r -> {
            r.flag();
            reviews.save(r);
            events.publishEvent(new ReviewFlagged(reviewId, r.getIsbn(), reason));
            log.warn("[REVIEWS] Review {} flagged: {}", reviewId, reason);
        });
    }

    @Transactional(readOnly = true)
    public List<Review> findByIsbn(String isbn) { return reviews.findByIsbn(isbn); }
}
