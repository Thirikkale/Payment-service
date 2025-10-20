
package com.thirikkale.payment_service.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.SetupIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import com.thirikkale.payment_service.dto.PaymentIntentRequest;
import com.thirikkale.payment_service.entity.Payment;
import com.thirikkale.payment_service.entity.PaymentMethod;
import com.thirikkale.payment_service.entity.Rider;
import com.thirikkale.payment_service.repository.PaymentMethodRepository;
import com.thirikkale.payment_service.repository.PaymentRepository;
import com.thirikkale.payment_service.repository.RiderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.stripe.param.SetupIntentCreateParams;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private StripeCustomerService stripeCustomerService;

    @Autowired
    private RiderRepository riderRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public String createSetupIntent(Long riderId) throws StripeException {
        // 1. Get the Stripe Customer ID for this rider
        String stripeCustomerId = stripeCustomerService.findOrCreateCustomer(riderId);

        // 2. Create the SetupIntent
        SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                .setCustomer(stripeCustomerId)
                .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION)

                // --- ADD THIS LINE ---
                // This tells Stripe we ONLY want to set up a card.
                .addPaymentMethodType("card")
                // ---------------------

                .build();

        SetupIntent setupIntent = SetupIntent.create(params);

        // 3. Return the client secret
        return setupIntent.getClientSecret();
    }

    @Transactional
    public PaymentIntent createPaymentIntent(PaymentIntentRequest request) throws StripeException {

        // 1. Find the Rider (mapping) from our local DB
        Rider rider = riderRepository.findById(request.getRiderId())
                .orElseThrow(() -> new RuntimeException("Rider not found"));

        // 2. Find the saved Payment Method from our local DB
        PaymentMethod paymentMethod = paymentMethodRepository.findByRiderId(request.getRiderId())
                .orElseThrow(() -> new RuntimeException("No saved payment method found for rider"));

        // 3. Set up the parameters to charge the card
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setCustomer(rider.getStripeCustomerId())
                .setPaymentMethod(paymentMethod.getStripePaymentMethodId())
                .setOffSession(true) // This is key for auto-charging
                .setConfirm(true)    // This tells Stripe to charge immediately
                .putMetadata("app_trip_id", request.getTripId().toString())
                .build();

        PaymentIntent paymentIntent;
        try {
            // 4. Create and confirm the charge in Stripe FIRST
            paymentIntent = PaymentIntent.create(params);
        } catch (StripeException e) {
            // If Stripe fails (e.g., bad card, network error),
            // we should still record the FAILED payment attempt.

            Payment payment = new Payment();
            payment.setRider(rider);
            payment.setTripId(request.getTripId());
            payment.setAmount(request.getAmount());
            payment.setCurrency(request.getCurrency());
            payment.setStatus(Payment.PaymentStatus.FAILED);
            // We set the Stripe ID to the exception message or a placeholder
            // This requires the column to be nullable.
            // Let's stick to the happy path for now and let it throw.
            throw e; // Re-throw the exception to the controller
        }

        // 5. NOW create and save our local Payment record
        // We only get here if the Stripe call returned an object
        Payment payment = new Payment();
        payment.setRider(rider);
        payment.setTripId(request.getTripId());
        payment.setAmount(paymentIntent.getAmount()); // Use amount from Stripe response
        payment.setCurrency(paymentIntent.getCurrency()); // Use currency from Stripe
        payment.setStripePaymentIntentId(paymentIntent.getId()); // <-- We HAVE the ID now

        // 6. Update our local Payment record with the final status from Stripe
        if ("succeeded".equals(paymentIntent.getStatus())) {
            payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
        }
        paymentRepository.save(payment);

        return paymentIntent;
    }
}