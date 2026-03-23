package com.ridex.trip.service;

import com.ridex.commons.dto.TripDTO;
import com.ridex.commons.events.MatchRequest;
import com.ridex.commons.events.TripEvent;
import com.ridex.commons.config.KafkaTopics;
import com.ridex.commons.exception.TripNotFoundException;
import com.ridex.trip.kafka.TripEventProducer;
import com.ridex.trip.model.Trip;
import com.ridex.trip.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class TripService {
    private static final Logger log = LoggerFactory.getLogger(TripService.class);
    private final TripRepository tripRepository;
    private final TripEventProducer tripEventProducer;
    private final KafkaTemplate<String, MatchRequest> matchRequestTemplate;

    public TripService(TripRepository tripRepository,
                       TripEventProducer tripEventProducer,
                       KafkaTemplate<String, MatchRequest> matchRequestTemplate) {
        this.tripRepository = tripRepository;
        this.tripEventProducer = tripEventProducer;
        this.matchRequestTemplate = matchRequestTemplate;
    }

    public Mono<TripDTO> requestTrip(UUID riderId, double pickupLat, double pickupLng,
                                      double dropoffLat, double dropoffLng, int fareEstimateCents) {
        Trip trip = new Trip();
        trip.setId(UUID.randomUUID());
        trip.setRiderId(riderId);
        trip.setStatus("REQUESTED");
        trip.setPickupLat(pickupLat);
        trip.setPickupLng(pickupLng);
        trip.setDropoffLat(dropoffLat);
        trip.setDropoffLng(dropoffLng);
        trip.setFareCents(fareEstimateCents);
        trip.setSurgeMultiplier(1.0);
        trip.setCreatedAt(Instant.now());

        return tripRepository.save(trip)
            .doOnSuccess(saved -> {
                tripEventProducer.publishTripEvent(toTripEvent(saved));
                matchRequestTemplate.send(KafkaTopics.MATCH_REQUESTS, riderId.toString(),
                    new MatchRequest(saved.getId(), riderId, pickupLat, pickupLng,
                        dropoffLat, dropoffLng, fareEstimateCents, Instant.now()));
                log.info("Trip requested: {}", saved.getId());
            })
            .map(this::toDTO);
    }

    public Mono<TripDTO> getTrip(UUID tripId) {
        return tripRepository.findById(tripId)
            .switchIfEmpty(Mono.error(new TripNotFoundException(tripId)))
            .map(this::toDTO);
    }

    public Flux<TripDTO> getTripsByRider(UUID riderId) {
        return tripRepository.findByRiderId(riderId).map(this::toDTO);
    }

    public Mono<TripDTO> updateTripStatus(UUID tripId, String newStatus) {
        return tripRepository.findById(tripId)
            .switchIfEmpty(Mono.error(new TripNotFoundException(tripId)))
            .flatMap(trip -> {
                trip.setStatus(newStatus);
                if ("IN_PROGRESS".equals(newStatus)) {
                    trip.setStartedAt(Instant.now());
                } else if ("COMPLETED".equals(newStatus)) {
                    trip.setCompletedAt(Instant.now());
                }
                return tripRepository.save(trip);
            })
            .doOnSuccess(trip -> tripEventProducer.publishTripEvent(toTripEvent(trip)))
            .map(this::toDTO);
    }

    public Mono<TripDTO> cancelTrip(UUID tripId) {
        return updateTripStatus(tripId, "CANCELLED");
    }

    private TripDTO toDTO(Trip t) {
        return new TripDTO(t.getId(), t.getRiderId(), t.getDriverId(), t.getStatus(),
            t.getPickupLat(), t.getPickupLng(), t.getDropoffLat(), t.getDropoffLng(),
            t.getFareCents() != null ? t.getFareCents() : 0,
            t.getSurgeMultiplier() != null ? t.getSurgeMultiplier() : 1.0,
            t.getStartedAt(), t.getCompletedAt(), t.getCreatedAt());
    }

    private TripEvent toTripEvent(Trip t) {
        return new TripEvent(t.getId(), t.getRiderId(), t.getDriverId(), t.getStatus(),
            t.getPickupLat(), t.getPickupLng(), t.getDropoffLat(), t.getDropoffLng(),
            t.getFareCents() != null ? t.getFareCents() : 0,
            t.getSurgeMultiplier() != null ? t.getSurgeMultiplier() : 1.0,
            Instant.now());
    }
}
