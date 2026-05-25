package io.eventus.examples.modulith.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    private String isbn;
    private int totalStock;
    private int reservedStock;

    protected InventoryItem() {}

    public InventoryItem(String isbn, int totalStock) {
        this.isbn = isbn;
        this.totalStock = totalStock;
        this.reservedStock = 0;
    }

    public boolean hasAvailable(int quantity) {
        return (totalStock - reservedStock) >= quantity;
    }

    public void reserve(int quantity) {
        if (!hasAvailable(quantity)) throw new IllegalStateException("Insufficient stock for " + isbn);
        this.reservedStock += quantity;
    }

    public void release(int quantity) {
        this.reservedStock = Math.max(0, this.reservedStock - quantity);
    }

    public String getIsbn() { return isbn; }
    public int getTotalStock() { return totalStock; }
    public int getReservedStock() { return reservedStock; }
    public int getAvailableStock() { return totalStock - reservedStock; }
}
