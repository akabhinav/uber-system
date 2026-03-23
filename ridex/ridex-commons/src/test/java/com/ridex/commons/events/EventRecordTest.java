package com.ridex.commons.events;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventRecordTest {

    // --- TripEvent ---

    @Test
    void tripEvent_constructionAndAccessors() {
        UUID tripId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        Instant now = Instant.now();

        TripEvent event = new TripEvent(
            tripId, riderId, driverId, "REQUESTED",
            40.7128, -74.0060, 40.7580, -73.9855,
            2500, 1.5, now
        );

        assertThat(event.tripId()).isEqualTo(tripId);
        assertThat(event.riderId()).isEqualTo(riderId);
        assertThat(event.driverId()).isEqualTo(driverId);
        assertThat(event.status()).isEqualTo("REQUESTED");
        assertThat(event.pickupLat()).isEqualTo(40.7128);
        assertThat(event.pickupLng()).isEqualTo(-74.0060);
        assertThat(event.dropoffLat()).isEqualTo(40.7580);
        assertThat(event.dropoffLng()).isEqualTo(-73.9855);
        assertThat(event.fareCents()).isEqualTo(2500);
        assertThat(event.surgeMultiplier()).isEqualTo(1.5);
        assertThat(event.timestamp()).isEqualTo(now);
    }

    @Test
    void tripEvent_equality() {
        UUID tripId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        Instant now = Instant.now();

        TripEvent a = new TripEvent(tripId, riderId, driverId, "COMPLETED", 0, 0, 0, 0, 1000, 1.0, now);
        TripEvent b = new TripEvent(tripId, riderId, driverId, "COMPLETED", 0, 0, 0, 0, 1000, 1.0, now);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    // --- LocationEvent ---

    @Test
    void locationEvent_constructionAndAccessors() {
        UUID driverId = UUID.randomUUID();
        Instant now = Instant.now();

        LocationEvent event = new LocationEvent(driverId, 37.7749, -122.4194, 45.5f, 270.0f, now);

        assertThat(event.driverId()).isEqualTo(driverId);
        assertThat(event.lat()).isEqualTo(37.7749);
        assertThat(event.lng()).isEqualTo(-122.4194);
        assertThat(event.speed()).isEqualTo(45.5f);
        assertThat(event.heading()).isEqualTo(270.0f);
        assertThat(event.timestamp()).isEqualTo(now);
    }

    @Test
    void locationEvent_equality() {
        UUID driverId = UUID.randomUUID();
        Instant now = Instant.now();

        LocationEvent a = new LocationEvent(driverId, 10.0, 20.0, 5.0f, 90.0f, now);
        LocationEvent b = new LocationEvent(driverId, 10.0, 20.0, 5.0f, 90.0f, now);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    // --- MatchRequest ---

    @Test
    void matchRequest_constructionAndAccessors() {
        UUID tripId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        Instant now = Instant.now();

        MatchRequest request = new MatchRequest(
            tripId, riderId, 40.7128, -74.0060, 40.7580, -73.9855, 3200, now
        );

        assertThat(request.tripId()).isEqualTo(tripId);
        assertThat(request.riderId()).isEqualTo(riderId);
        assertThat(request.pickupLat()).isEqualTo(40.7128);
        assertThat(request.pickupLng()).isEqualTo(-74.0060);
        assertThat(request.dropoffLat()).isEqualTo(40.7580);
        assertThat(request.dropoffLng()).isEqualTo(-73.9855);
        assertThat(request.fareEstimateCents()).isEqualTo(3200);
        assertThat(request.timestamp()).isEqualTo(now);
    }

    @Test
    void matchRequest_equality() {
        UUID tripId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        Instant now = Instant.now();

        MatchRequest a = new MatchRequest(tripId, riderId, 0, 0, 0, 0, 1000, now);
        MatchRequest b = new MatchRequest(tripId, riderId, 0, 0, 0, 0, 1000, now);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    // --- MatchResult ---

    @Test
    void matchResult_constructionAndAccessors() {
        UUID tripId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        Instant now = Instant.now();

        MatchResult result = new MatchResult(
            tripId, riderId, driverId, 40.72, -74.01, 180, 2.5, true, null, now
        );

        assertThat(result.tripId()).isEqualTo(tripId);
        assertThat(result.riderId()).isEqualTo(riderId);
        assertThat(result.driverId()).isEqualTo(driverId);
        assertThat(result.driverLat()).isEqualTo(40.72);
        assertThat(result.driverLng()).isEqualTo(-74.01);
        assertThat(result.etaSeconds()).isEqualTo(180);
        assertThat(result.distanceKm()).isEqualTo(2.5);
        assertThat(result.matched()).isTrue();
        assertThat(result.failureReason()).isNull();
        assertThat(result.timestamp()).isEqualTo(now);
    }

    @Test
    void matchResult_failedMatch() {
        UUID tripId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        Instant now = Instant.now();

        MatchResult result = new MatchResult(
            tripId, riderId, null, 0, 0, 0, 0, false, "No drivers available", now
        );

        assertThat(result.matched()).isFalse();
        assertThat(result.failureReason()).isEqualTo("No drivers available");
        assertThat(result.driverId()).isNull();
    }

    @Test
    void matchResult_equality() {
        UUID tripId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        Instant now = Instant.now();

        MatchResult a = new MatchResult(tripId, riderId, driverId, 0, 0, 60, 1.0, true, null, now);
        MatchResult b = new MatchResult(tripId, riderId, driverId, 0, 0, 60, 1.0, true, null, now);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    // --- PaymentEvent ---

    @Test
    void paymentEvent_constructionAndAccessors() {
        UUID paymentId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        Instant now = Instant.now();

        PaymentEvent event = new PaymentEvent(
            paymentId, tripId, riderId, driverId, 2500, "USD", "CAPTURED", "pi_abc123", now
        );

        assertThat(event.paymentId()).isEqualTo(paymentId);
        assertThat(event.tripId()).isEqualTo(tripId);
        assertThat(event.riderId()).isEqualTo(riderId);
        assertThat(event.driverId()).isEqualTo(driverId);
        assertThat(event.amountCents()).isEqualTo(2500);
        assertThat(event.currency()).isEqualTo("USD");
        assertThat(event.status()).isEqualTo("CAPTURED");
        assertThat(event.stripePaymentIntentId()).isEqualTo("pi_abc123");
        assertThat(event.timestamp()).isEqualTo(now);
    }

    @Test
    void paymentEvent_equality() {
        UUID paymentId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        Instant now = Instant.now();

        PaymentEvent a = new PaymentEvent(paymentId, tripId, riderId, driverId, 1000, "USD", "CAPTURED", "pi_1", now);
        PaymentEvent b = new PaymentEvent(paymentId, tripId, riderId, driverId, 1000, "USD", "CAPTURED", "pi_1", now);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
