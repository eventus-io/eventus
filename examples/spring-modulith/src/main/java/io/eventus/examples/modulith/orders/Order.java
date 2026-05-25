package io.eventus.examples.modulith.orders;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String id;
    private String isbn;
    private String customerId;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    protected Order() {}

    public Order(String id, String isbn, String customerId, int quantity) {
        this.id = id;
        this.isbn = isbn;
        this.customerId = customerId;
        this.quantity = quantity;
        this.status = OrderStatus.PENDING;
    }

    public void confirm() { this.status = OrderStatus.CONFIRMED; }
    public void cancel() { this.status = OrderStatus.CANCELLED; }

    public String getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getCustomerId() { return customerId; }
    public int getQuantity() { return quantity; }
    public OrderStatus getStatus() { return status; }
}
