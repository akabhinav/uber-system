package com.ridex.payment.service;

import com.ridex.commons.config.KafkaTopics;
import com.ridex.commons.events.PaymentEvent;
import com.ridex.commons.events.TripEvent;
import com.ridex.commons.exception.PaymentFailedException;
import com.ridex.payment.model.Payment;
import com.ridex.payment.repository.PaymentRepository;
import com.ridex.payment.stripe.StripeGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StripeGateway stripeGateway;

    @Mock
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    private PaymentService paymentService;

    private UUID tripId;
    private UUID riderId;
    private UUID driverId;
    private TripEvent tripEvent;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, stripeGateway, kafkaTemplate);
        tripId = UUID.randomUUID();
        riderId = UUID.randomUUID();
        driverId = UUID.randomUUID();
        tripEvent = new TripEvent(
            tripId, riderId, driverId, "COMPLETED",
            40.7128, -74.0060, 40.7580, -73.9855,
            2500, 1.0, Instant.now()
        );
    }

    @Test
    void processPaymentForTrip_existingPayment_returnsExisting() {
        String idempotencyKey = "trip-" + tripId.toString();
        Payment existing = new Payment();
        existing.setId(UUID.randomUUID());
        existing.setTripId(tripId);
        existing.setStatus("CAPTURED");
        existing.setIdempotencyKey(idempotencyKey);

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
            .thenReturn(Mono.just(existing));

        StepVerifier.create(paymentService.processPaymentForTrip(tripEvent))
            .assertNext(payment -> {
                assertThat(payment.getStatus()).isEqualTo("CAPTURED");
                assertThat(payment.getTripId()).isEqualTo(tripId);
            })
            .verifyComplete();

        verify(stripeGateway, never()).createPaymentIntent(anyInt(), anyString(), anyString());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPaymentForTrip_newPayment_createsAndSaves() {
        String idempotencyKey = "trip-" + tripId.toString();
        String intentId = "pi_test_123";

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
            .thenReturn(Mono.empty());
        when(stripeGateway.createPaymentIntent(2500, "usd", idempotencyKey))
            .thenReturn(intentId);
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(paymentService.processPaymentForTrip(tripEvent))
            .assertNext(payment -> {
                assertThat(payment.getTripId()).isEqualTo(tripId);
                assertThat(payment.getAmountCents()).isEqualTo(2500);
                assertThat(payment.getCurrency()).isEqualTo("USD");
                assertThat(payment.getStatus()).isEqualTo("CAPTURED");
                assertThat(payment.getStripePaymentIntentId()).isEqualTo(intentId);
                assertThat(payment.getIdempotencyKey()).isEqualTo(idempotencyKey);
            })
            .verifyComplete();

        verify(kafkaTemplate).send(eq(KafkaTopics.PAYMENT_EVENTS), eq(tripId.toString()), any(PaymentEvent.class));
    }

    @Test
    void processPaymentForTrip_stripeFails_savesFailedPayment() {
        String idempotencyKey = "trip-" + tripId.toString();

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
            .thenReturn(Mono.empty());
        when(stripeGateway.createPaymentIntent(2500, "usd", idempotencyKey))
            .thenThrow(new PaymentFailedException("card declined"));
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(paymentService.processPaymentForTrip(tripEvent))
            .assertNext(payment -> {
                assertThat(payment.getStatus()).isEqualTo("FAILED");
                assertThat(payment.getStripePaymentIntentId()).isNull();
            })
            .verifyComplete();

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
    }

    @Test
    void processPaymentForTrip_publishesKafkaEvent() {
        String idempotencyKey = "trip-" + tripId.toString();

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
            .thenReturn(Mono.empty());
        when(stripeGateway.createPaymentIntent(anyInt(), anyString(), anyString()))
            .thenReturn("pi_test_456");
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(paymentService.processPaymentForTrip(tripEvent))
            .expectNextCount(1)
            .verifyComplete();

        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.PAYMENT_EVENTS), eq(tripId.toString()), eventCaptor.capture());

        PaymentEvent published = eventCaptor.getValue();
        assertThat(published.tripId()).isEqualTo(tripId);
        assertThat(published.riderId()).isEqualTo(riderId);
        assertThat(published.driverId()).isEqualTo(driverId);
        assertThat(published.amountCents()).isEqualTo(2500);
        assertThat(published.status()).isEqualTo("CAPTURED");
    }

    @Test
    void getPaymentByTripId_delegatesToRepository() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setTripId(tripId);

        when(paymentRepository.findByTripId(tripId)).thenReturn(Mono.just(payment));

        StepVerifier.create(paymentService.getPaymentByTripId(tripId))
            .assertNext(p -> assertThat(p.getTripId()).isEqualTo(tripId))
            .verifyComplete();

        verify(paymentRepository).findByTripId(tripId);
    }

    @Test
    void getPaymentByTripId_notFound_returnsEmpty() {
        when(paymentRepository.findByTripId(tripId)).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.getPaymentByTripId(tripId))
            .verifyComplete();
    }
}
