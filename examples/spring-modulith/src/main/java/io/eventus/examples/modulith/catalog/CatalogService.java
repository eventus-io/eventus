package io.eventus.examples.modulith.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final BookRepository books;

    public CatalogService(BookRepository books) {
        this.books = books;
    }

    public List<Book> findAll() {
        return books.findAll();
    }

    public Optional<Book> findByIsbn(String isbn) {
        return books.findById(isbn);
    }
}
