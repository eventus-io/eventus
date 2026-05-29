package io.eventus.examples.modulith;

import io.eventus.examples.modulith.catalog.Book;
import io.eventus.examples.modulith.catalog.BookRepository;
import io.eventus.examples.modulith.catalog.CatalogService;
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
    private final CatalogService catalog;

    DataInitializer(BookRepository books, InventoryRepository inventory,
                    OrderService orders, CatalogService catalog) {
        this.books = books;
        this.inventory = inventory;
        this.orders = orders;
        this.catalog = catalog;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Seed catalog
        books.save(new Book("978-0-13-468599-1", "Clean Architecture",         "Robert C. Martin",        44.99));
        books.save(new Book("978-0-13-235088-4", "The Pragmatic Programmer",   "David Thomas",            49.99));
        books.save(new Book("978-0-13-110362-7", "The C Programming Language", "Kernighan & Ritchie",     39.99));
        books.save(new Book("978-0-13-468599-2", "Domain-Driven Design",       "Eric Evans",              54.99));
        books.save(new Book("978-0-13-468599-3", "Designing Data-Intensive Applications", "Martin Kleppmann", 59.99));

        // Seed inventory (separate from catalog stock)
        inventory.save(new InventoryItem("978-0-13-468599-1", 20));
        inventory.save(new InventoryItem("978-0-13-235088-4", 15));
        inventory.save(new InventoryItem("978-0-13-110362-7", 3));  // low stock — will deplete quickly
        inventory.save(new InventoryItem("978-0-13-468599-2", 12));
        inventory.save(new InventoryItem("978-0-13-468599-3", 8));

        // Publish two books so BookPublished events appear in the graph
        catalog.publish("978-0-13-468599-1");
        catalog.publish("978-0-13-235088-4");

        // Trigger a price drop so PriceChanged events appear
        catalog.updatePrice("978-0-13-110362-7", 29.99);

        // Seed orders — mix of normal and 7-multiple (triggers PaymentFailed)
        orders.place("978-0-13-468599-1", 2, "customer-001");
        orders.place("978-0-13-235088-4", 1, "customer-002");
        orders.place("978-0-13-468599-2", 7, "customer-003");  // qty=7 → PaymentFailed
        orders.place("978-0-13-468599-3", 3, "customer-004");
        orders.place("978-0-13-110362-7", 3, "customer-005");  // exhausts the 3 copies
    }
}
