package io.eventus.examples.modulith.loyalty;

import jakarta.persistence.*;

@Entity
@Table(name = "loyalty_accounts")
class LoyaltyAccount {

    @Id private String customerId;
    private int points;

    protected LoyaltyAccount() {}

    LoyaltyAccount(String customerId) { this.customerId = customerId; this.points = 0; }

    void award(int pts)  { this.points += pts; }
    void redeem(int pts) { this.points = Math.max(0, this.points - pts); }

    String getCustomerId() { return customerId; }
    int getPoints()        { return points; }
}
