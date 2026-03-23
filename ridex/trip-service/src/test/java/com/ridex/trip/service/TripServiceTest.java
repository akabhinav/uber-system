package com.ridex.trip.service;

import com.ridex.commons.config.KafkaTopics;
import com.ridex.commons.dto.TripDTO;
import com.ridex.commons.events.MatchRequest;
import com.ridex.commons.events.TripEvent;
import com.ridex.commons.exception.TripNotFoundException;
import com.ridex.trip.kafka.TripEventProducer;
import com.ridex.trip.model.Trip;
import com.ridex.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripEventProducer tripEventProducer;

    @Mock
    private KafkaTemplate<String, MatchRequest> matchRequestTemplate;

    @Captor
    private ArgumentCaptor<Trip> tripCaptor;

    @Captor
    private ArgumentCaptor<TripEvent> tripEventCaptor;

    @Captor
    private ArgumentCaptor<MatchRequest> matchRequestCaptor;

    private TripService tripService;

    private static final UUID RIDER_ID = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();
    private static final UUID TRIP_ID = UUID.randomUUID();
    private static final double PICKUP_LAT = 37.7749;
    private static final double PICKUP_LNG = -122.4194;
    private static final double DROPOFF_LAT = 37.3382;
    private static final double DROPOFF_LNG = -121.8863;
    private static final int FARE_CENTS = 2500;

    @BeforeEach
    void setUp() {
        tripService = new TripService(tripRepository, tripEventProducer, matchRequestTemplate);
    }

    private Trip buildTrip(UUID id, UUID riderId, String status) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setRiderId(riderId);
        trip.setStatus(status);
        trip.setPickupLat(PICKUP_LAT);
        trip.setPickupLng(PICKUP_LNG);
        trip.setDropoffLat(DROPOFF_LAT);
        trip.setDropoffLng(DROPOFF_LNG);
        trip.setFareCents(FARE_CENTS);
        trip.setSurgeMultiplier(1.0);
        trip.setCreatedAt(Instant.now());
        return trip;
    }

    @Nested
    @DisplayName("requestTrip")
    class RequestTrip {

        @Test
        @DisplayName("should create a trip with REQUESTED status and publish events")
        void shouldCreateTripAndPublishEvents() {
            when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(tripService.requestTrip(RIDER_ID, PICKUP_LAT, PICKUP_LNG,
                    DROPOFF_LAT, DROPOFF_LNG, FARE_CENTS))
                .assertNext(dto -> {
                    assertThat(dto.id()).isNotNull();
                    assertThat(dto.riderId()).isEqualTo(RIDER_ID);
                    assertThat(dto.status()).isEqualTo("REQUESTED");
                    assertThat(dto.pickupLat()).isEqualTo(PICKUP_LAT);
                    assertThat(dto.pickupLng()).isEqualTo(PICKUP_LNG);
                    assertThat(dto.dropoffLat()).isEqualTo(DROPOFF_LAT);
                    assertThat(dto.dropoffLng()).isEqualTo(DROPOFF_LNG);
                    assertThat(dto.fareCents()).isEqualTo(FARE_CENTS);
                    assertThat(dto.surgeMultiplier()).isEqualTo(1.0);
                    assertThat(dto.createdAt()).isNotNull();
                })
                .verifyComplete();

            verify(tripRepository).save(tripCaptor.capture());
            Trip savedTrip = tripCaptor.getValue();
            assertThat(savedTrip.getStatus()).isEqualTo("REQUESTED");
            assertThat(savedTrip.getRiderId()).isEqualTo(RIDER_ID);

            verify(tripEventProducer).publishTripEvent(any(TripEvent.class));
            verify(matchRequestTemplate).send(eq(KafkaTopics.MATCH_REQUESTS),
                    eq(RIDER_ID.toString()), any(MatchRequest.class));
        }

        @Test
        @DisplayName("should set default surge multiplier to 1.0")
        void shouldSetDefaultSurgeMultiplier() {
            when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(tripService.requestTrip(RIDER_ID, PICKUP_LAT, PICKUP_LNG,
                    DROPOFF_LAT, DROPOFF_LNG, FARE_CENTS))
                .assertNext(dto -> assertThat(dto.surgeMultiplier()).isEqualTo(1.0))
                .verifyComplete();
        }

        @Test
        @DisplayName("should propagate repository save error")
        void shouldPropagateRepositoryError() {
            when(tripRepository.save(any(Trip.class))).thenReturn(Mono.error(new RuntimeException("DB error")));

            StepVerifier.create(tripService.requestTrip(RIDER_ID, PICKUP_LAT, PICKUP_LNG,
                    DROPOFF_LAT, DROPOFF_LNG, FARE_CENTS))
                .expectErrorMatches(ex -> ex instanceof RuntimeException && ex.getMessage().equals("DB error"))
                .verify();
        }
    }

    @Nested
    @DisplayName("getTrip")
    class GetTrip {

        @Test
        @DisplayName("should return trip when found")
        void shouldReturnTripWhenFound() {
            Trip trip = buildTrip(TRIP_ID, RIDER_ID, "REQUESTED");
            when(tripRepository.findById(TRIP_ID)).thenReturn(Mono.just(trip));

            StepVerifier.create(tripService.getTrip(TRIP_ID))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(TRIP_ID);
                    assertThat(dto.riderId()).isEqualTo(RIDER_ID);
                    assertThat(dto.status()).isEqualTo("REQUESTED");
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should throw TripNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Mono.empty());

            StepVerifier.create(tripService.getTrip(TRIP_ID))
                .expectError(TripNotFoundException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("getTripsByRider")
    class GetTripsByRider {

        @Test
        @DisplayName("should return all trips for a rider")
        void shouldReturnAllTripsForRider() {
            Trip trip1 = buildTrip(UUID.randomUUID(), RIDER_ID, "COMPLETED");
            Trip trip2 = buildTrip(UUID.randomUUID(), RIDER_ID, "REQUESTED");
            when(tripRepository.findByRiderId(RIDER_ID)).thenReturn(Flux.just(trip1, trip2));

            StepVerifier.create(tripService.getTripsByRider(RIDER_ID))
                .assertNext(dto -> assertThat(dto.status()).isEqualTo("COMPLETED"))
                .assertNext(dto -> assertThat(dto.status()).isEqualTo("REQUESTED"))
                .verifyComplete();
        }

        @Test
        @DisplayName("should return empty flux when rider has no trips")
        void shouldReturnEmptyWhenNoTrips() {
            when(tripRepository.findByRiderId(RIDER_ID)).thenReturn(Flux.empty());

            StepVerifier.create(tripService.getTripsByRider(RIDER_ID))
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("updateTripStatus")
    class UpdateTripStatus {

        @Test
        @DisplayName("should update status and publish event")
        void shouldUpdateStatusAndPublishEvent() {
            Trip trip = buildTrip(TRIP_ID, RIDER_ID, "REQUESTED");
            when(tripRepository.findById(TRIP_ID)).thenReturn(Mono.just(trip));
            when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(tripService.updateTripStatus(TRIP_ID, "MATCHED"))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(TRIP_ID);
                    assertThat(dto.status()).isEqualTo("MATCHED");
                })
                .verifyComplete();

            verify(tripEventProducer).publishTripEvent(any(TripEvent.class));
        }

        @Test
        @DisplayName("should set startedAt when transitioning to IN_PROGRESS")
        void shouldSetStartedAtForInProgress() {
            Trip trip = buildTrip(TRIP_ID, RIDER_ID, "PICKUP");
            when(tripRepository.findById(TRIP_ID)).thenReturn(Mono.just(trip));
            when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(tripService.updateTripStatus(TRIP_ID, "IN_PROGRESS"))
                .assertNext(dto -> {
                    assertThat(dto.status()).isEqualTo("IN_PROGRESS");
                    assertThat(dto.startedAt()).isNotNull();
                })
                .verifyComplete();

            verify(tripRepository).save(tripCaptor.capture());
            assertThat(tripCaptor.getValue().getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("should set completedAt when transitioning to COMPLETED")
        void shouldSetCompletedAtForCompleted() {
            Trip trip = buildTrip(TRIP_ID, RIDER_ID, "IN_PROGRESS");
            trip.setStartedAt(Instant.now());
            when(tripRepository.findById(TRIP_ID)).thenReturn(Mono.just(trip));
            when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(tripService.updateTripStatus(TRIP_ID, "COMPLETED"))
                .assertNext(dto -> {
                    assertThat(dto.status()).isEqualTo("COMPLETED");
                    assertThat(dto.completedAt()).isNotNull();
                })
                .verifyComplete();

            verify(tripRepository).save(tripCaptor.capture());
            assertThat(tripCaptor.getValue().getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw TripNotFoundException when trip does not exist")
        void shouldThrowWhenTripNotFound() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Mono.empty());

            StepVerifier.create(tripService.updateTripStatus(TRIP_ID, "MATCHED"))
                .expectError(TripNotFoundException.class)
                .verify();

            verify(tripRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not set startedAt or completedAt for other status transitions")
        void shouldNotSetTimestampsForOtherStatuses() {
            Trip trip = buildTrip(TRIP_ID, RIDER_ID, "REQUESTED");
            when(tripRepository.findById(TRIP_ID)).thenReturn(Mono.just(trip));
            when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(tripService.updateTripStatus(TRIP_ID, "MATCHED"))
                .assertNext(dto -> {
                    assertThat(dto.startedAt()).isNull();
                    assertThat(dto.completedAt()).isNull();
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("cancelTrip")
    class CancelTrip {

        @Test
        @DisplayName("should set status to CANCELLED")
        void shouldSetStatusToCancelled() {
            Trip trip = buildTrip(TRIP_ID, RIDER_ID, "REQUESTED");
            when(tripRepository.findById(TRIP_ID)).thenReturn(Mono.just(trip));
            when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(tripService.cancelTrip(TRIP_ID))
                .assertNext(dto -> assertThat(dto.status()).isEqualTo("CANCELLED"))
                .verifyComplete();

            verify(tripEventProducer).publishTripEvent(any(TripEvent.class));
        }

        @Test
        @DisplayName("should throw TripNotFoundException when trip does not exist")
        void shouldThrowWhenTripNotFound() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Mono.empty());

            StepVerifier.create(tripService.cancelTrip(TRIP_ID))
                .expectError(TripNotFoundException.class)
                .verify();
        }
    }
}
