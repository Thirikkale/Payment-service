
package com.thirikkale.payment_service.repository;

import com.thirikkale.payment_service.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    /**
     * Finds a payment method for a rider.
     */
    Optional<PaymentMethod> findByRiderId(Long riderId);
    // -----------------------
}