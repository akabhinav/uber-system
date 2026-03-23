package com.ridex.payment.repository;

import com.ridex.payment.model.Payment;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PaymentRepository extends R2dbcRepository<Payment, UUID> {
    Mono<Payment> findByTripId(UUID tripId);
    Mono<Payment> findByIdempotencyKey(String idempotencyKey);
}
