package com.thirikkale.payment_service.repository;

// Import from your new entity package
import com.thirikkale.payment_service.entity.Rider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RiderRepository extends JpaRepository<Rider, Long> {
    Optional<Rider> findByStripeCustomerId(String stripeCustomerId);
}