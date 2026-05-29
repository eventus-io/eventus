package io.eventus.examples.modulith.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;

interface LoyaltyRepository extends JpaRepository<LoyaltyAccount, String> {}
