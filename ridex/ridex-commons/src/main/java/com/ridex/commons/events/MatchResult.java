package com.ridex.commons.events;

import java.time.Instant;
import java.util.UUID;

public record MatchResult(
    UUID tripId,
    UUID riderId,
    UUID driverId,
    double driverLat,
    double driverLng,
    int etaSeconds,
    double distanceKm,
    boolean matched,
    String failureReason,
    Instant timestamp
) {}
