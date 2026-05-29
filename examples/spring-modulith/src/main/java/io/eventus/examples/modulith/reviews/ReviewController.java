package io.eventus.examples.modulith.reviews;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
class ReviewController {

    private final ReviewService reviewService;

    ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{isbn}")
    List<Review> byIsbn(@PathVariable String isbn) {
        return reviewService.findByIsbn(isbn);
    }

    @PostMapping
    ResponseEntity<Review> post(@RequestParam String isbn,
                                @RequestParam String customerId,
                                @RequestParam int rating,
                                @RequestParam(defaultValue = "") String comment) {
        return ResponseEntity.ok(reviewService.post(isbn, customerId, rating, comment));
    }

    @PostMapping("/{reviewId}/flag")
    ResponseEntity<Void> flag(@PathVariable String reviewId, @RequestParam String reason) {
        reviewService.flag(reviewId, reason);
        return ResponseEntity.ok().build();
    }
}
