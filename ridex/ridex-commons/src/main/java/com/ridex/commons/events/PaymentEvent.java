package com.ridex.commons.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentEvent(
    UUID paymentId,
    UUID tripId,
    UUID riderId,
    UUID driverId,
    int amountCents,
    String currency,
    String status,
    String stripePaymentIntentId,
    Instant timestamp
) {}
