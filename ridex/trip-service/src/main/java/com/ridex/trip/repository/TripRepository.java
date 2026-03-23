package com.ridex.trip.repository;

import com.ridex.trip.model.Trip;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface TripRepository extends R2dbcRepository<Trip, UUID> {
    Flux<Trip> findByRiderId(UUID riderId);
    Flux<Trip> findByDriverId(UUID driverId);
    Flux<Trip> findByStatus(String status);
}
