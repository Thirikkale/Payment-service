//This is what our API will send back to Flutter. It contains the clientSecret that the Flutter Stripe SDK needs to complete the card setup
package com.thirikkale.payment_service.dto;

import lombok.Data;

@Data
public class SetupIntentResponse {
    private String clientSecret;

    // A constructor to make it easy to create this response
    public SetupIntentResponse(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}