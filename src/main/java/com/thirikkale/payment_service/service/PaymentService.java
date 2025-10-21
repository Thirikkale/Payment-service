package com.thirikkale.payment_service.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.SetupIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import com.thirikkale.payment_service.dto.PaymentIntentRequest;
import com.thirikkale.payment_service.dto.PaymentMethodResponse;
import com.thirikkale.payment_service.entity.Payment;
import com.thirikkale.payment_service.entity.PaymentMethod;
import com.thirikkale.payment_service.entity.Rider;
import com.thirikkale.payment_service.repository.PaymentMethodRepository;
import com.thirikkale.payment_service.repository.PaymentRepository;
import com.thirikkale.payment_service.repository.RiderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    @Autowired
    private StripeCustomerService stripeCustomerService;

    @Autowired
    private RiderRepository riderRepository; // Assumes JpaRepository<Rider, String>

    @Autowired
    private PaymentMethodRepository paymentMethodRepository; // Assumes methods now use String riderId if needed

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Creates a Stripe SetupIntent for a given rider.
     * Accepts riderId as String.
     */
    public String createSetupIntent(String riderIdStr) throws StripeException {
        // 1. Get the Stripe Customer ID (Passes the String ID to the updated service)
        // Assumes stripeCustomerService.findOrCreateCustomer handles String ID correctly
        String stripeCustomerId = stripeCustomerService.findOrCreateCustomer(riderIdStr);

        // 2. Create the SetupIntent
        SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                .setCustomer(stripeCustomerId)
                .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION)
                .addPaymentMethodType("card") // Specify only card
                .build();

        SetupIntent setupIntent = SetupIntent.create(params);

        // 3. Return the client secret
        return setupIntent.getClientSecret();
    }

    /**
     * Creates and confirms a PaymentIntent to charge a rider's saved card.
     * ASSUMPTION: PaymentIntentRequest DTO has been updated to use 'String riderId'.
     * Update PaymentIntentRequest.java accordingly.
     */
    @Transactional
    public PaymentIntent createPaymentIntent(PaymentIntentRequest request) throws StripeException {

        // --- Use String riderId directly from request ---
        String riderIdStr = request.getRiderId();
        if (riderIdStr == null || riderIdStr.isEmpty()) {
            throw new IllegalArgumentException("Rider ID cannot be null or empty in PaymentIntentRequest");
        }
        // ----------------------------------------------

        // 1. Find the Rider (mapping) from our local DB using String ID
        Rider rider = riderRepository.findById(riderIdStr) // Use String ID
                .orElseThrow(() -> new RuntimeException("Rider mapping not found for ID: " + riderIdStr));

        // 2. Find the saved Payment Method from our local DB using String ID
        // --- ASSUMPTION: paymentMethodRepository.findByRiderId now accepts String ---
        // You MUST update PaymentMethodRepository to add:
        // Optional<PaymentMethod> findByRider_Id(String riderId); OR adjust based on your entity mapping
        // Let's assume the method is findByRiderId accepting String for simplicity
        PaymentMethod paymentMethod = paymentMethodRepository.findByRiderId(riderIdStr) // Use String ID
                .orElseThrow(() -> new RuntimeException("No saved payment method found for rider ID: " + riderIdStr));
        // --------------------------------------------------------------------------

        // 3. Set up the parameters to charge the card (No changes needed here)
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setCustomer(rider.getStripeCustomerId())
                .setPaymentMethod(paymentMethod.getStripePaymentMethodId())
                .setOffSession(true)
                .setConfirm(true)
                .putMetadata("app_trip_id", request.getTripId().toString())
                .build();

        PaymentIntent paymentIntent;
        try {
            // 4. Create and confirm the charge in Stripe FIRST
            paymentIntent = PaymentIntent.create(params);
        } catch (StripeException e) {
            // Record a FAILED payment attempt locally
            Payment payment = new Payment();
            payment.setRider(rider); // Assumes Payment entity has @ManyToOne Rider rider
            payment.setTripId(request.getTripId());
            payment.setAmount(request.getAmount());
            payment.setCurrency(request.getCurrency());
            payment.setStatus(Payment.PaymentStatus.FAILED);
            // Ensure 'stripePaymentIntentId' column is nullable in Payment entity.
            payment.setStripePaymentIntentId(null);
            paymentRepository.save(payment);

            throw e; // Re-throw the exception
        }

        // 5. Create and save our local Payment record on success/attempt
        Payment payment = new Payment();
        payment.setRider(rider); // Assumes Payment entity has @ManyToOne Rider rider
        payment.setTripId(request.getTripId());
        payment.setAmount(paymentIntent.getAmount());
        payment.setCurrency(paymentIntent.getCurrency());
        payment.setStripePaymentIntentId(paymentIntent.getId());

        // 6. Update local status based on Stripe's response
        if ("succeeded".equals(paymentIntent.getStatus())) {
            payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
        }
        paymentRepository.save(payment);

        return paymentIntent;
    }

    /**
     * Retrieves a list of saved payment methods for a given rider.
     * Accepts riderId as String.
     */
    public List<PaymentMethodResponse> getSavedPaymentMethods(String riderIdStr) {

        // --- REMOVED PARSING LOGIC ---
        // Long riderIdLong; try { ... } catch { ... }
        // -----------------------------

        // 1. Verify the rider mapping exists using the String ID
        riderRepository.findById(riderIdStr) // Use String ID
                .orElseThrow(() -> new RuntimeException("Rider mapping not found for ID: " + riderIdStr));

        // 2. Find all cards for that rider using the String ID
        // --- ASSUMPTION: paymentMethodRepository.findAllByRiderId now accepts String ---
        // You MUST update PaymentMethodRepository to add:
        // List<PaymentMethod> findAllByRider_Id(String riderId); OR adjust based on your entity mapping
        // Let's assume the method is findAllByRiderId accepting String
        List<PaymentMethod> methods = paymentMethodRepository.findAllByRiderId(riderIdStr); // Use String ID
        // -----------------------------------------------------------------------------

        // 3. Convert the list of entities to a list of DTOs
        return methods.stream()
                .map(PaymentMethodResponse::fromEntity)
                .collect(Collectors.toList());
    }
}