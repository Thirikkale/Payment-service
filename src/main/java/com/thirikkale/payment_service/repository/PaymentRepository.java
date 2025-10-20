package com.thirikkale.payment_service.repository;

// Import from your new entity package
import com.thirikkale.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {}