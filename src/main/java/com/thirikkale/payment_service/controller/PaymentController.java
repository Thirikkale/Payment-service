//This is the main controller that your Flutter app will communicate with.

package com.thirikkale.payment_service.controller;

import com.stripe.model.PaymentIntent; // <-- Import this
import com.thirikkale.payment_service.dto.PaymentIntentRequest;
import com.thirikkale.payment_service.dto.SetupIntentRequest;
import com.thirikkale.payment_service.dto.SetupIntentResponse;
import com.thirikkale.payment_service.service.PaymentService;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/setup-intent")
    public ResponseEntity<?> createSetupIntent(@RequestBody SetupIntentRequest request) {
        try {
            String clientSecret = paymentService.createSetupIntent(request.getRiderId());
            return ResponseEntity.ok(new SetupIntentResponse(clientSecret));
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    /**
     * Endpoint to call when a trip is completed to charge the rider's saved card.
     */
    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentIntentRequest request) {
        try {
            PaymentIntent paymentIntent = paymentService.createPaymentIntent(request);
            // Return just the status for simplicity
            return ResponseEntity.ok(paymentIntent.getStatus());
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            // Catches "Rider not found" or "No payment method"
            return ResponseEntity.status(404).body(e.getMessage());
        }
        // ---------------------------------------------
    }
}