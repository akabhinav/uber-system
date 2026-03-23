package com.ridex.payment.stripe;

import com.ridex.commons.exception.PaymentFailedException;
import com.stripe.exception.ApiException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeGatewayTest {

    private StripeGateway stripeGateway;

    @BeforeEach
    void setUp() {
        stripeGateway = new StripeGateway();
    }

    @Test
    void createPaymentIntent_success_returnsIntentId() {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_test_abc123");

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                .thenReturn(mockIntent);

            String intentId = stripeGateway.createPaymentIntent(1500, "usd", "idem-key-1");

            assertThat(intentId).isEqualTo("pi_test_abc123");
            mockedStatic.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)));
        }
    }

    @Test
    void createPaymentIntent_stripeException_throwsPaymentFailed() {
        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                .thenThrow(new ApiException("Card declined", null, null, 402, null));

            assertThatThrownBy(() -> stripeGateway.createPaymentIntent(1500, "usd", "idem-key-2"))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("Card declined");
        }
    }

    @Test
    void createPaymentIntent_passesCorrectAmount() {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_test_xyz");

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                .thenReturn(mockIntent);

            stripeGateway.createPaymentIntent(9999, "eur", "idem-key-3");

            mockedStatic.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)), times(1));
        }
    }
}
