// src/main/java/com/thirikkale/payment_service/service/WebhookService.java

package com.thirikkale.payment_service.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod; // <-- Import this
import com.stripe.model.SetupIntent;
import com.thirikkale.payment_service.entity.Rider;
import com.thirikkale.payment_service.repository.PaymentMethodRepository;
import com.thirikkale.payment_service.repository.RiderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

    @Autowired
    private RiderRepository riderRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    /**
     * Called when a 'setup_intent.succeeded' event is received.
     * This means the user has successfully added a new card.
     * We now need to save a reference to it in our database.
     */
    @Transactional
    public void handleSetupIntentSucceeded(SetupIntent setupIntent) {

        // 1. Get the Stripe Customer ID from the SetupIntent
        String stripeCustomerId = setupIntent.getCustomer();

        // 2. Find the corresponding Rider in our local database
        Rider rider = riderRepository.findByStripeCustomerId(stripeCustomerId)
                .orElseThrow(() -> {
                    // This should never happen if our Phase 2 logic is correct
                    logger.error("FATAL: No rider found for stripe_customer_id: {}", stripeCustomerId);
                    return new RuntimeException("Rider not found for customer ID");
                });

        // 3. Get the PaymentMethod ID from the SetupIntent
        String stripePaymentMethodId = setupIntent.getPaymentMethod();

        // 4. Retrieve the full PaymentMethod object from Stripe
        // We do this to get the card's brand and last4 digits
        PaymentMethod stripePaymentMethod;
        try {
            stripePaymentMethod = PaymentMethod.retrieve(stripePaymentMethodId);
        } catch (StripeException e) {
            logger.error("Failed to retrieve PaymentMethod from Stripe: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        // 5. Create our new local PaymentMethod entity
        com.thirikkale.payment_service.entity.PaymentMethod newMethod =
                new com.thirikkale.payment_service.entity.PaymentMethod();

        newMethod.setRider(rider);
        newMethod.setStripePaymentMethodId(stripePaymentMethodId);
        newMethod.setBrand(stripePaymentMethod.getCard().getBrand());
        newMethod.setLast4(stripePaymentMethod.getCard().getLast4());

        // TODO: Add logic to set is_default=true.
        // For now, we'll just set it to true.
        // In a real app, you'd set other cards for this rider to false.
        newMethod.setDefault(true);

        // 6. Save the new payment method
        paymentMethodRepository.save(newMethod);

        logger.info("Successfully saved new payment method {} for rider {}",
                stripePaymentMethodId, rider.getId());
    }
}