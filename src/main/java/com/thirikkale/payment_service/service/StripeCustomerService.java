//find a rider in our database and make sure they have a stripe_customer_id. If they don't, it will create one in Stripe and save the new ID.

package com.thirikkale.payment_service.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import com.thirikkale.payment_service.entity.Rider;
import com.thirikkale.payment_service.repository.RiderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class StripeCustomerService {

    @Autowired
    private RiderRepository riderRepository;

    /**
     * Finds or creates a local Rider mapping entry and ensures a Stripe Customer ID exists.
     * This service assumes the riderId is valid as it comes from another service.
     */
    @Transactional
    public String findOrCreateCustomer(Long riderId) throws StripeException {

        // 1. Try to find the rider mapping in this service's DB
        Optional<Rider> optionalRider = riderRepository.findById(riderId);

        if (optionalRider.isPresent()) {
            // 2. RIDER MAPPING EXISTS
            // this record is guaranteed to have a stripeCustomerId from a previous successful run.
            return optionalRider.get().getStripeCustomerId();
        }

        // 3. RIDER MAPPING DOES NOT EXIST (First time seeing this rider)
        // 3a. Create a new Customer in Stripe
        CustomerCreateParams params = CustomerCreateParams.builder()
                .putMetadata("app_rider_id", riderId.toString())
                .build();
        Customer customer = Customer.create(params);
        String stripeId = customer.getId();

        // 3b. Create and save the new local mapping entry
        Rider newRiderMapping = new Rider();
        newRiderMapping.setId(riderId);
        newRiderMapping.setStripeCustomerId(stripeId);
        riderRepository.save(newRiderMapping);

        // 3c. Return the new ID
        return stripeId;
    }
}