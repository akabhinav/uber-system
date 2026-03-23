package com.ridex.commons.dto;

import java.util.UUID;

public record DriverDTO(
    UUID id,
    UUID userId,
    String name,
    String licenseNo,
    double rating,
    boolean available,
    double currentLat,
    double currentLng
) {}
