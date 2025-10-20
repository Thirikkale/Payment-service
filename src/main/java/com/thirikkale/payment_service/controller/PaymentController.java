//This is the main controller that your Flutter app will communicate with.

package com.thirikkale.payment_service.controller;

import com.stripe.model.PaymentIntent;
import com.thirikkale.payment_service.dto.PaymentIntentRequest;
import com.thirikkale.payment_service.dto.SetupIntentRequest;
import com.thirikkale.payment_service.dto.SetupIntentResponse;
import com.thirikkale.payment_service.service.PaymentService;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.thirikkale.payment_service.dto.PaymentMethodResponse;
import java.util.List;

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

    @GetMapping("/methods/{riderId}")
    public ResponseEntity<List<PaymentMethodResponse>> getPaymentMethods(@PathVariable Long riderId) {
        try {
            List<PaymentMethodResponse> methods = paymentService.getSavedPaymentMethods(riderId);
            return ResponseEntity.ok(methods);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(null); // e.g., Rider not found
        }
    }
}