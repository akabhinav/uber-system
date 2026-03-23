package com.ridex.commons.events;

import java.time.Instant;
import java.util.UUID;

public record LocationEvent(
    UUID driverId,
    double lat,
    double lng,
    float speed,
    float heading,
    Instant timestamp
) {}
