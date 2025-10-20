
package com.thirikkale.payment_service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "payments")
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false, unique = true)
    private Long tripId;

    @ManyToOne
    @JoinColumn(name = "rider_id", nullable = false)
    private Rider rider;

    @Column(name = "stripe_payment_intent_id", unique = true, nullable = false)
    private String stripePaymentIntentId;

    private Long amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    public enum PaymentStatus {
        PENDING, SUCCEEDED, FAILED
    }
}