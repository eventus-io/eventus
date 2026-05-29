package io.eventus.examples.modulith.reviews;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reviews")
class Review {

    @Id private String id;
    private String isbn;
    private String customerId;
    private int rating;
    private String comment;
    private boolean flagged;
    private Instant postedAt;

    protected Review() {}

    Review(String id, String isbn, String customerId, int rating, String comment) {
        this.id = id; this.isbn = isbn; this.customerId = customerId;
        this.rating = rating; this.comment = comment;
        this.flagged = false; this.postedAt = Instant.now();
    }

    void flag() { this.flagged = true; }

    public String getId()         { return id; }
    public String getIsbn()       { return isbn; }
    public String getCustomerId() { return customerId; }
    public int getRating()        { return rating; }
    public String getComment()    { return comment; }
    public boolean isFlagged()    { return flagged; }
    public Instant getPostedAt()  { return postedAt; }
}
