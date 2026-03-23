package com.ridex.trip.controller;

import com.ridex.commons.dto.TripDTO;
import com.ridex.commons.exception.TripNotFoundException;
import com.ridex.trip.service.TripService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WebFluxTest(TripController.class)
class TripControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TripService tripService;

    private static final UUID TRIP_ID = UUID.randomUUID();
    private static final UUID RIDER_ID = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();
    private static final double PICKUP_LAT = 37.7749;
    private static final double PICKUP_LNG = -122.4194;
    private static final double DROPOFF_LAT = 37.3382;
    private static final double DROPOFF_LNG = -121.8863;
    private static final int FARE_CENTS = 2500;

    private TripDTO buildTripDTO(UUID id, String status) {
        return new TripDTO(id, RIDER_ID, DRIVER_ID, status,
                PICKUP_LAT, PICKUP_LNG, DROPOFF_LAT, DROPOFF_LNG,
                FARE_CENTS, 1.0, null, null, Instant.now());
    }

    @Nested
    @DisplayName("POST /api/trips")
    class RequestTrip {

        @Test
        @DisplayName("should create trip and return 200 with trip data")
        void shouldCreateTrip() {
            TripDTO tripDTO = buildTripDTO(TRIP_ID, "REQUESTED");
            when(tripService.requestTrip(eq(RIDER_ID), eq(PICKUP_LAT), eq(PICKUP_LNG),
                    eq(DROPOFF_LAT), eq(DROPOFF_LNG), eq(FARE_CENTS)))
                .thenReturn(Mono.just(tripDTO));

            webTestClient.post().uri("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "riderId": "%s",
                        "pickupLat": %f,
                        "pickupLng": %f,
                        "dropoffLat": %f,
                        "dropoffLng": %f,
                        "fareEstimateCents": %d
                    }
                    """.formatted(RIDER_ID, PICKUP_LAT, PICKUP_LNG,
                        DROPOFF_LAT, DROPOFF_LNG, FARE_CENTS))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(TRIP_ID.toString())
                .jsonPath("$.data.riderId").isEqualTo(RIDER_ID.toString())
                .jsonPath("$.data.status").isEqualTo("REQUESTED");
        }

        @Test
        @DisplayName("should return error when service fails")
        void shouldReturnErrorOnServiceFailure() {
            when(tripService.requestTrip(any(), anyDouble(), anyDouble(),
                    anyDouble(), anyDouble(), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

            webTestClient.post().uri("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "riderId": "%s",
                        "pickupLat": 37.7749,
                        "pickupLng": -122.4194,
                        "dropoffLat": 37.3382,
                        "dropoffLng": -121.8863,
                        "fareEstimateCents": 2500
                    }
                    """.formatted(RIDER_ID))
                .exchange()
                .expectStatus().is5xxServerError();
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}")
    class GetTrip {

        @Test
        @DisplayName("should return trip when found")
        void shouldReturnTrip() {
            TripDTO tripDTO = buildTripDTO(TRIP_ID, "MATCHED");
            when(tripService.getTrip(TRIP_ID)).thenReturn(Mono.just(tripDTO));

            webTestClient.get().uri("/api/trips/{tripId}", TRIP_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(TRIP_ID.toString())
                .jsonPath("$.data.status").isEqualTo("MATCHED");
        }

        @Test
        @DisplayName("should return error when trip not found")
        void shouldReturnErrorWhenNotFound() {
            when(tripService.getTrip(TRIP_ID))
                .thenReturn(Mono.error(new TripNotFoundException(TRIP_ID)));

            webTestClient.get().uri("/api/trips/{tripId}", TRIP_ID)
                .exchange()
                .expectStatus().is5xxServerError();
        }
    }

    @Nested
    @DisplayName("GET /api/trips/rider/{riderId}")
    class GetRiderTrips {

        @Test
        @DisplayName("should return list of trips for rider")
        void shouldReturnRiderTrips() {
            TripDTO trip1 = buildTripDTO(UUID.randomUUID(), "COMPLETED");
            TripDTO trip2 = buildTripDTO(UUID.randomUUID(), "REQUESTED");
            when(tripService.getTripsByRider(RIDER_ID)).thenReturn(Flux.just(trip1, trip2));

            webTestClient.get().uri("/api/trips/rider/{riderId}", RIDER_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.length()").isEqualTo(2)
                .jsonPath("$.data[0].status").isEqualTo("COMPLETED")
                .jsonPath("$.data[1].status").isEqualTo("REQUESTED");
        }

        @Test
        @DisplayName("should return empty list when rider has no trips")
        void shouldReturnEmptyList() {
            when(tripService.getTripsByRider(RIDER_ID)).thenReturn(Flux.empty());

            webTestClient.get().uri("/api/trips/rider/{riderId}", RIDER_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.length()").isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("PATCH /api/trips/{tripId}/status")
    class UpdateStatus {

        @Test
        @DisplayName("should update trip status")
        void shouldUpdateStatus() {
            TripDTO tripDTO = buildTripDTO(TRIP_ID, "DRIVER_EN_ROUTE");
            when(tripService.updateTripStatus(TRIP_ID, "DRIVER_EN_ROUTE"))
                .thenReturn(Mono.just(tripDTO));

            webTestClient.patch().uri("/api/trips/{tripId}/status", TRIP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {"status": "DRIVER_EN_ROUTE"}
                    """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.status").isEqualTo("DRIVER_EN_ROUTE");
        }

        @Test
        @DisplayName("should return error when trip not found")
        void shouldReturnErrorWhenNotFound() {
            when(tripService.updateTripStatus(eq(TRIP_ID), anyString()))
                .thenReturn(Mono.error(new TripNotFoundException(TRIP_ID)));

            webTestClient.patch().uri("/api/trips/{tripId}/status", TRIP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {"status": "MATCHED"}
                    """)
                .exchange()
                .expectStatus().is5xxServerError();
        }
    }

    @Nested
    @DisplayName("POST /api/trips/{tripId}/cancel")
    class CancelTrip {

        @Test
        @DisplayName("should cancel trip and return cancelled status")
        void shouldCancelTrip() {
            TripDTO tripDTO = buildTripDTO(TRIP_ID, "CANCELLED");
            when(tripService.cancelTrip(TRIP_ID)).thenReturn(Mono.just(tripDTO));

            webTestClient.post().uri("/api/trips/{tripId}/cancel", TRIP_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.status").isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("should return error when trip not found")
        void shouldReturnErrorWhenNotFound() {
            when(tripService.cancelTrip(TRIP_ID))
                .thenReturn(Mono.error(new TripNotFoundException(TRIP_ID)));

            webTestClient.post().uri("/api/trips/{tripId}/cancel", TRIP_ID)
                .exchange()
                .expectStatus().is5xxServerError();
        }
    }
}
