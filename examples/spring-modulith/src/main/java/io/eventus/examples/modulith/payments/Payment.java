package io.eventus.examples.modulith.payments;

import jakarta.persistence.*;

@Entity
@Table(name = "payments")
class Payment {

    @Id private String id;
    private String orderId;
    private String customerId;
    private double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    protected Payment() {}

    Payment(String id, String orderId, String customerId, double amount) {
        this.id = id; this.orderId = orderId;
        this.customerId = customerId; this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    void authorize() { this.status = PaymentStatus.AUTHORIZED; }
    void fail()      { this.status = PaymentStatus.FAILED; }
    void refund()    { this.status = PaymentStatus.REFUNDED; }

    String getOrderId()    { return orderId; }
    String getCustomerId() { return customerId; }
    double getAmount()     { return amount; }
}
