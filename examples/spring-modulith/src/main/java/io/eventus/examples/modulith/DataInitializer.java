package io.eventus.examples.modulith;

import io.eventus.examples.modulith.catalog.Book;
import io.eventus.examples.modulith.catalog.BookRepository;
import io.eventus.examples.modulith.inventory.InventoryItem;
import io.eventus.examples.modulith.inventory.InventoryRepository;
import io.eventus.examples.modulith.orders.OrderService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
class DataInitializer implements ApplicationRunner {

    private final BookRepository books;
    private final InventoryRepository inventory;
    private final OrderService orders;

    DataInitializer(BookRepository books, InventoryRepository inventory, OrderService orders) {
        this.books = books;
        this.inventory = inventory;
        this.orders = orders;
    }

    @Override
    public void run(ApplicationArguments args) {
        books.save(new Book("978-0-13-468599-1", "Clean Architecture", "Robert C. Martin", 20));
        books.save(new Book("978-0-13-235088-4", "The Pragmatic Programmer", "David Thomas", 15));
        books.save(new Book("978-0-13-110362-7", "The C Programming Language", "Kernighan & Ritchie", 10));

        inventory.save(new InventoryItem("978-0-13-468599-1", 20));
        inventory.save(new InventoryItem("978-0-13-235088-4", 15));
        inventory.save(new InventoryItem("978-0-13-110362-7", 10));

        // Seed a couple of orders so event publications appear immediately
        orders.place("978-0-13-468599-1", 2, "customer-001");
        orders.place("978-0-13-235088-4", 1, "customer-002");
    }
}
