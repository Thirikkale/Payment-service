
package com.thirikkale.payment_service.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    // This @Value annotation now reads the hardcoded key from your application.yml
    @Value("${stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = secretKey;
    }
}