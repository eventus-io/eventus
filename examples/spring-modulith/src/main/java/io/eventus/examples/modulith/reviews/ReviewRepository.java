package io.eventus.examples.modulith.reviews;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ReviewRepository extends JpaRepository<Review, String> {
    List<Review> findByIsbn(String isbn);
}
