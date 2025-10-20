
package com.thirikkale.payment_service.dto;

import com.thirikkale.payment_service.entity.PaymentMethod;
import lombok.Data;

@Data
public class PaymentMethodResponse {

    // We send the Stripe ID, not our internal DB ID
    private String stripePaymentMethodId;
    private String brand;
    private String last4;
    private boolean isDefault;

    // A helper to convert our Entity to this DTO
    public static PaymentMethodResponse fromEntity(PaymentMethod entity) {
        PaymentMethodResponse dto = new PaymentMethodResponse();
        dto.setStripePaymentMethodId(entity.getStripePaymentMethodId());
        dto.setBrand(entity.getBrand());
        dto.setLast4(entity.getLast4());
        dto.setDefault(entity.isDefault());
        return dto;
    }
}