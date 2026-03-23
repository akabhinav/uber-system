package com.ridex.commons.events;

import java.time.Instant;
import java.util.UUID;

public record MatchRequest(
    UUID tripId,
    UUID riderId,
    double pickupLat,
    double pickupLng,
    double dropoffLat,
    double dropoffLng,
    int fareEstimateCents,
    Instant timestamp
) {}
