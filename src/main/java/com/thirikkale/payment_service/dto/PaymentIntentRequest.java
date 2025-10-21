//This class will define the JSON body sent when a trip is finished and needs to be paid for.

package com.thirikkale.payment_service.dto;

import lombok.Data;

@Data
public class PaymentIntentRequest {
    private Long tripId;
    private String riderId;
    private Long amount; // Amount in cents (e.g., 1000 for $10.00)
    private String currency; // e.g., "lkr", "usd"
}