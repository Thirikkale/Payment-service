//This class will define the JSON body that Flutter sends when a rider wants to save a card. It just needs the rider's ID.

package com.thirikkale.payment_service.dto;

import lombok.Data;

@Data
public class SetupIntentRequest {
    private String riderId;
}