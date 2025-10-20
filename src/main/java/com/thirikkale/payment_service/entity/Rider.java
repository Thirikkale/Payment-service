
package com.thirikkale.payment_service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "riders")
@Data
public class Rider {
    @Id
    private Long id;

    @Column(name = "stripe_customer_id", unique = true)
    private String stripeCustomerId;
}