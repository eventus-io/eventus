package io.eventus.examples.modulith.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
public class Book {

    @Id
    private String isbn;
    private String title;
    private String author;
    private double price;

    protected Book() {}

    public Book(String isbn, String title, String author, double price) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.price = price;
    }

    public String getIsbn()   { return isbn; }
    public String getTitle()  { return title; }
    public String getAuthor() { return author; }
    public double getPrice()  { return price; }

    void setPrice(double price) { this.price = price; }
}
