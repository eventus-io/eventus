package io.eventus.examples.modulith.catalog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog/books")
class CatalogController {

    private final CatalogService catalog;

    CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    List<Book> listBooks() {
        return catalog.findAll();
    }

    @PostMapping("/{isbn}/publish")
    ResponseEntity<Void> publish(@PathVariable String isbn) {
        catalog.publish(isbn);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{isbn}/price")
    ResponseEntity<Void> updatePrice(@PathVariable String isbn, @RequestParam double price) {
        catalog.updatePrice(isbn, price);
        return ResponseEntity.ok().build();
    }
}
