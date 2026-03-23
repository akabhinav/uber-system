package com.ridex.payment.stripe;

import com.ridex.commons.exception.PaymentFailedException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeGateway {
    private static final Logger log = LoggerFactory.getLogger(StripeGateway.class);

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe gateway initialized");
    }

    public String createPaymentIntent(int amountCents, String currency, String idempotencyKey) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) amountCents)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();

            PaymentIntent intent = PaymentIntent.create(params);
            log.info("Created payment intent: {}", intent.getId());
            return intent.getId();
        } catch (StripeException e) {
            log.error("Stripe payment failed: {}", e.getMessage());
            throw new PaymentFailedException(e.getMessage());
        }
    }
}
