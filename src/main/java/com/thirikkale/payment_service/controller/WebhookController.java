//doesn't get called by the Flutter app. It gets called by Stripe's servers to notify you about events (like "Payment succeeded" or "Payment failed").

// src/main/java/com/thirikkale/payment_service/controller/WebhookController.java

package com.thirikkale.payment_service.controller;

import com.google.gson.JsonSyntaxException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.thirikkale.payment_service.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Autowired
    private WebhookService webhookService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader)
    {
        Event event;

        // 1. Verify the signature (same as before)
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (JsonSyntaxException e) {
            logger.warn("Webhook Error: Invalid JSON payload.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        } catch (SignatureVerificationException e) {
            logger.warn("Webhook Error: Invalid signature.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        // 2. Get the event data object DIRECTLY (This is the new part)
        StripeObject stripeObject = event.getData().getObject();

        // Check if the object is null (which would be strange, but safe to check)
        if (stripeObject == null) {
            logger.warn("Webhook Error: Event data object is null for event type {}", event.getType());
            // We return 200 OK so Stripe doesn't retry this bad event.
            return ResponseEntity.ok().body("Event data was null, but acknowledged.");
        }

        // 3. Handle only the events we care about
        switch (event.getType()) {
            case "setup_intent.succeeded":
                logger.info("Webhook: Received setup_intent.succeeded!");
                // Cast the generic object to the specific type we expect
                SetupIntent setupIntent = (SetupIntent) stripeObject;
                webhookService.handleSetupIntentSucceeded(setupIntent);
                break;

            case "payment_intent.succeeded":
                logger.info("Webhook: Received payment_intent.succeeded!");
                // We'll add this handler later if needed
                break;

            case "payment_intent.payment_failed":
                logger.info("Webhook: Received payment_intent.payment_failed.");
                // We'll add this handler later if needed
                break;

            default:
                // --- This is the IMPORTANT part ---
                // We log it, but we don't return an error.
                logger.info("Webhook: Received unhandled event type: {}", event.getType());
                // Events like 'customer.created' and 'payment_method.attached'
                // will end up here, which is fine.
        }

        // 4. Always return 200 OK to Stripe
        // This tells Stripe "We got it, thank you, please don't send it again."
        return ResponseEntity.ok().body("Event received");
    }
}