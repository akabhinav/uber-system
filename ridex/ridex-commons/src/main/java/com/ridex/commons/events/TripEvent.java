package com.ridex.commons.events;

import java.time.Instant;
import java.util.UUID;

public record TripEvent(
    UUID tripId,
    UUID riderId,
    UUID driverId,
    String status,
    double pickupLat,
    double pickupLng,
    double dropoffLat,
    double dropoffLng,
    int fareCents,
    double surgeMultiplier,
    Instant timestamp
) {}
