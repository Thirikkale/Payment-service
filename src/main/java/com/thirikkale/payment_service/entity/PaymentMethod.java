
package com.thirikkale.payment_service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "payment_methods")
@Data
public class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "rider_id", nullable = false)
    private Rider rider;

    @Column(name = "stripe_payment_method_id", unique = true, nullable = false)
    private String stripePaymentMethodId;

    private String brand;
    private String last4;

    @Column(name = "is_default")
    private boolean isDefault;
}