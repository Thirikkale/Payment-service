//This is the main controller that your Flutter app will communicate with.

package com.thirikkale.payment_service.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.thirikkale.payment_service.dto.PaymentIntentRequest;
import com.thirikkale.payment_service.dto.PaymentMethodResponse;
import com.thirikkale.payment_service.dto.SetupIntentRequest;
import com.thirikkale.payment_service.dto.SetupIntentResponse;
import com.thirikkale.payment_service.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Endpoint for Flutter to call when a user wants to save a new card.
     * Accepts riderId as a String.
     */
    @PostMapping("/setup-intent")
    public ResponseEntity<?> createSetupIntent(@RequestBody SetupIntentRequest request) {
        System.out.println("Received /setup-intent request for riderId: " + request.getRiderId()); // Add this
        // The SetupIntentRequest DTO now expects a String riderId
        try {
            // Assume paymentService.createSetupIntent now accepts a String
            String clientSecret = paymentService.createSetupIntent(request.getRiderId());
            return ResponseEntity.ok(new SetupIntentResponse(clientSecret));
        } catch (StripeException e) {
            // Handle Stripe API errors (e.g., invalid key)
            return ResponseEntity.badRequest().body("Stripe error: " + e.getMessage());
        } catch (RuntimeException e) {
            // Handle service layer errors (e.g., invalid rider ID format for DB lookup, rider not found conceptually)
            // Return 400 for bad input data or 404 if rider truly doesn't exist.
            // use 400 for potential ID format issues from the service.
            return ResponseEntity.status(400).body("Error processing request: " + e.getMessage());
        }
    }

    /**
     * Endpoint to call when a trip is completed to charge the rider's saved card.
     * NOTE: This endpoint likely still expects riderId as Long in PaymentIntentRequest.
     * If the Ride Service sends the riderId as String here too, you'll need to update
     * PaymentIntentRequest DTO and potentially the PaymentService.createPaymentIntent method similarly.
     * Assuming Ride Service sends Long for now.
     */
    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentIntentRequest request) {
        try {
            PaymentIntent paymentIntent = paymentService.createPaymentIntent(request);
            // Return just the status for simplicity
            return ResponseEntity.ok(paymentIntent.getStatus());
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Stripe error: " + e.getMessage());
        } catch (RuntimeException e) {
            // Catches "Rider not found" or "No payment method" from service layer
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    /**
     * Endpoint to get saved payment methods for a rider.
     * Accepts riderId as a String path variable.
     */
    @GetMapping("/methods/{riderId}")
    public ResponseEntity<List<PaymentMethodResponse>> getPaymentMethods(@PathVariable String riderId) { // <-- Changed from Long to String
        try {
            // Assume paymentService.getSavedPaymentMethods now accepts a String
            List<PaymentMethodResponse> methods = paymentService.getSavedPaymentMethods(riderId);
            return ResponseEntity.ok(methods);
        } catch (RuntimeException e) {
            // Catches "Rider not found" or potentially invalid ID format from service layer
            // Return 404 for "not found" or 400 for bad format. Let's keep 404 for simplicity.
            return ResponseEntity.status(404).body(null);
        }
    }
}