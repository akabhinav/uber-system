package com.ridex.location.service;

import com.ridex.commons.events.LocationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocationFanoutServiceTest {

    private LocationFanoutService fanoutService;
    private UUID driverId;

    @BeforeEach
    void setUp() throws Exception {
        fanoutService = new LocationFanoutService();
        driverId = UUID.randomUUID();

        // Set the bufferSize field via reflection since @Value won't be processed
        Field bufferSizeField = LocationFanoutService.class.getDeclaredField("bufferSize");
        bufferSizeField.setAccessible(true);
        bufferSizeField.setInt(fanoutService, 1024);
    }

    @Test
    void subscribe_createsFluxForDriver() {
        Flux<LocationEvent> flux = fanoutService.subscribe(driverId.toString());
        assertThat(flux).isNotNull();
    }

    @Test
    void broadcast_deliversEventToSubscriber() {
        LocationEvent event = new LocationEvent(driverId, 40.7128, -74.0060, 30.0f, 180.0f, Instant.now());

        Flux<LocationEvent> flux = fanoutService.subscribe(driverId.toString());

        StepVerifier.create(flux.take(1))
            .then(() -> fanoutService.broadcast(event).subscribe())
            .assertNext(received -> {
                assertThat(received.driverId()).isEqualTo(driverId);
                assertThat(received.lat()).isEqualTo(40.7128);
                assertThat(received.lng()).isEqualTo(-74.0060);
                assertThat(received.speed()).isEqualTo(30.0f);
                assertThat(received.heading()).isEqualTo(180.0f);
            })
            .verifyComplete();
    }

    @Test
    void broadcast_noSubscriber_completesWithoutError() {
        UUID unknownDriver = UUID.randomUUID();
        LocationEvent event = new LocationEvent(unknownDriver, 40.0, -74.0, 0f, 0f, Instant.now());

        StepVerifier.create(fanoutService.broadcast(event))
            .verifyComplete();
    }

    @Test
    void broadcast_multipleEvents_allDelivered() {
        LocationEvent event1 = new LocationEvent(driverId, 40.71, -74.00, 25.0f, 90.0f, Instant.now());
        LocationEvent event2 = new LocationEvent(driverId, 40.72, -74.01, 30.0f, 95.0f, Instant.now());
        LocationEvent event3 = new LocationEvent(driverId, 40.73, -74.02, 35.0f, 100.0f, Instant.now());

        Flux<LocationEvent> flux = fanoutService.subscribe(driverId.toString());

        StepVerifier.create(flux.take(3))
            .then(() -> {
                fanoutService.broadcast(event1).subscribe();
                fanoutService.broadcast(event2).subscribe();
                fanoutService.broadcast(event3).subscribe();
            })
            .assertNext(e -> assertThat(e.lat()).isEqualTo(40.71))
            .assertNext(e -> assertThat(e.lat()).isEqualTo(40.72))
            .assertNext(e -> assertThat(e.lat()).isEqualTo(40.73))
            .verifyComplete();
    }

    @Test
    void subscribe_differentDrivers_independentStreams() {
        UUID driver2 = UUID.randomUUID();

        LocationEvent event1 = new LocationEvent(driverId, 40.71, -74.00, 25.0f, 90.0f, Instant.now());
        LocationEvent event2 = new LocationEvent(driver2, 41.88, -87.63, 20.0f, 45.0f, Instant.now());

        Flux<LocationEvent> flux1 = fanoutService.subscribe(driverId.toString());
        Flux<LocationEvent> flux2 = fanoutService.subscribe(driver2.toString());

        StepVerifier.create(flux1.take(1))
            .then(() -> {
                fanoutService.broadcast(event1).subscribe();
                fanoutService.broadcast(event2).subscribe();
            })
            .assertNext(e -> assertThat(e.driverId()).isEqualTo(driverId))
            .verifyComplete();

        StepVerifier.create(flux2.take(1))
            .then(() -> fanoutService.broadcast(event2).subscribe())
            .assertNext(e -> assertThat(e.driverId()).isEqualTo(driver2))
            .verifyComplete();
    }
}
