package io.eventus.examples.modulith.catalog;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final BookRepository books;
    private final ApplicationEventPublisher events;

    public CatalogService(BookRepository books, ApplicationEventPublisher events) {
        this.books = books;
        this.events = events;
    }

    public List<Book> findAll() {
        return books.findAll();
    }

    public Optional<Book> findByIsbn(String isbn) {
        return books.findById(isbn);
    }

    @Transactional
    public void publish(String isbn) {
        books.findById(isbn).ifPresent(b ->
            events.publishEvent(new BookPublished(b.getIsbn(), b.getTitle(), b.getAuthor(), b.getPrice()))
        );
    }

    @Transactional
    public void updatePrice(String isbn, double newPrice) {
        books.findById(isbn).ifPresent(b -> {
            double old = b.getPrice();
            b.setPrice(newPrice);
            books.save(b);
            events.publishEvent(new PriceChanged(isbn, old, newPrice));
        });
    }
}
