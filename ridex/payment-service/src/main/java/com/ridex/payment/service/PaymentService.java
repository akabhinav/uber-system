package com.ridex.payment.service;

import com.ridex.commons.config.KafkaTopics;
import com.ridex.commons.events.PaymentEvent;
import com.ridex.commons.events.TripEvent;
import com.ridex.commons.exception.PaymentFailedException;
import com.ridex.payment.model.Payment;
import com.ridex.payment.repository.PaymentRepository;
import com.ridex.payment.stripe.StripeGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;
    private final StripeGateway stripeGateway;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public PaymentService(PaymentRepository paymentRepository,
                          StripeGateway stripeGateway,
                          KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.stripeGateway = stripeGateway;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<Payment> processPaymentForTrip(TripEvent tripEvent) {
        String idempotencyKey = "trip-" + tripEvent.tripId().toString();
        log.info("Processing payment for trip={}, amount={}",
            tripEvent.tripId(), tripEvent.fareCents());

        return paymentRepository.findByIdempotencyKey(idempotencyKey)
            .switchIfEmpty(Mono.defer(() -> createPayment(tripEvent, idempotencyKey)));
    }

    private Mono<Payment> createPayment(TripEvent tripEvent, String idempotencyKey) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setTripId(tripEvent.tripId());
        payment.setAmountCents(tripEvent.fareCents());
        payment.setCurrency("USD");
        payment.setStatus("PENDING");
        payment.setIdempotencyKey(idempotencyKey);
        payment.setCreatedAt(Instant.now());

        try {
            String intentId = stripeGateway.createPaymentIntent(
                tripEvent.fareCents(), "usd", idempotencyKey);
            payment.setStripePaymentIntentId(intentId);
            payment.setStatus("CAPTURED");
        } catch (PaymentFailedException e) {
            payment.setStatus("FAILED");
            log.error("Payment failed for trip={}", tripEvent.tripId(), e);
        }

        return paymentRepository.save(payment)
            .doOnSuccess(saved -> {
                PaymentEvent event = new PaymentEvent(
                    saved.getId(), saved.getTripId(), tripEvent.riderId(),
                    tripEvent.driverId(), saved.getAmountCents(), saved.getCurrency(),
                    saved.getStatus(), saved.getStripePaymentIntentId(), Instant.now());
                kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS,
                    tripEvent.tripId().toString(), event);
            });
    }

    public Mono<Payment> getPaymentByTripId(UUID tripId) {
        return paymentRepository.findByTripId(tripId);
    }
}
