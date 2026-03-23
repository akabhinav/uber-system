package com.ridex.commons.dto;

import java.time.Instant;
import java.util.UUID;

public record TripDTO(
    UUID id,
    UUID riderId,
    UUID driverId,
    String status,
    double pickupLat,
    double pickupLng,
    double dropoffLat,
    double dropoffLng,
    int fareCents,
    double surgeMultiplier,
    Instant startedAt,
    Instant completedAt,
    Instant createdAt
) {}
