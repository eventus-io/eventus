package io.eventus.examples.modulith.payments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrderId(String orderId);
}
