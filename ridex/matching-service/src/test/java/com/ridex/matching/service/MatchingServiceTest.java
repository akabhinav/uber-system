package com.ridex.matching.service;

import com.ridex.commons.config.KafkaTopics;
import com.ridex.commons.events.MatchRequest;
import com.ridex.commons.events.MatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @Mock
    private KafkaTemplate<String, MatchResult> kafkaTemplate;

    private MatchingService matchingService;

    private static final UUID TRIP_ID = UUID.randomUUID();
    private static final UUID RIDER_ID = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        matchingService = new MatchingService(redisTemplate, kafkaTemplate);

        // Set @Value fields via reflection
        Field searchRadiusField = MatchingService.class.getDeclaredField("searchRadiusKm");
        searchRadiusField.setAccessible(true);
        searchRadiusField.set(matchingService, 5.0);

        Field maxCandidatesField = MatchingService.class.getDeclaredField("maxCandidates");
        maxCandidatesField.setAccessible(true);
        maxCandidatesField.set(matchingService, 10);
    }

    private MatchRequest createMatchRequest() {
        return new MatchRequest(
            TRIP_ID, RIDER_ID,
            40.7128, -74.0060,   // pickup
            40.7580, -73.9855,   // dropoff
            2500, Instant.now()
        );
    }

    // ── findMatch with drivers available ──

    @Test
    void findMatch_driversAvailable_returnsMatchedResult() {
        MatchRequest request = createMatchRequest();

        // Build a geo result for the nearest driver
        Point driverPoint = new Point(-74.0000, 40.7200);
        RedisGeoCommands.GeoLocation<String> geoLocation =
            new RedisGeoCommands.GeoLocation<>(DRIVER_ID.toString(), driverPoint);
        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult =
            new GeoResult<>(geoLocation, new Distance(1.2, Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
            new GeoResults<>(List.of(geoResult));

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.radius(eq("driver:locations"), any(Circle.class),
            any(RedisGeoCommands.GeoRadiusCommandArgs.class))).thenReturn(geoResults);

        MatchResult result = matchingService.findMatch(request);

        assertTrue(result.matched());
        assertEquals(TRIP_ID, result.tripId());
        assertEquals(RIDER_ID, result.riderId());
        assertEquals(DRIVER_ID, result.driverId());
        assertNull(result.failureReason());
    }

    @Test
    void findMatch_driversAvailable_calculatesEtaFromDistance() {
        MatchRequest request = createMatchRequest();

        double distanceKm = 2.5;
        Point driverPoint = new Point(-74.0000, 40.7200);
        RedisGeoCommands.GeoLocation<String> geoLocation =
            new RedisGeoCommands.GeoLocation<>(DRIVER_ID.toString(), driverPoint);
        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult =
            new GeoResult<>(geoLocation, new Distance(distanceKm, Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
            new GeoResults<>(List.of(geoResult));

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.radius(eq("driver:locations"), any(Circle.class),
            any(RedisGeoCommands.GeoRadiusCommandArgs.class))).thenReturn(geoResults);

        MatchResult result = matchingService.findMatch(request);

        // ETA formula: (distanceKm / 0.5) * 60
        int expectedEta = (int) (distanceKm / 0.5 * 60);
        assertEquals(expectedEta, result.etaSeconds());
        assertEquals(distanceKm, result.distanceKm(), 0.001);
    }

    @Test
    void findMatch_driversAvailable_setsDriverCoordinates() {
        MatchRequest request = createMatchRequest();

        Point driverPoint = new Point(-74.0050, 40.7150);
        RedisGeoCommands.GeoLocation<String> geoLocation =
            new RedisGeoCommands.GeoLocation<>(DRIVER_ID.toString(), driverPoint);
        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult =
            new GeoResult<>(geoLocation, new Distance(0.8, Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
            new GeoResults<>(List.of(geoResult));

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.radius(eq("driver:locations"), any(Circle.class),
            any(RedisGeoCommands.GeoRadiusCommandArgs.class))).thenReturn(geoResults);

        MatchResult result = matchingService.findMatch(request);

        // Point.getY() = lat, Point.getX() = lng
        assertEquals(driverPoint.getY(), result.driverLat(), 0.0001);
        assertEquals(driverPoint.getX(), result.driverLng(), 0.0001);
    }

    @Test
    void findMatch_driversAvailable_sendsKafkaMessage() {
        MatchRequest request = createMatchRequest();

        Point driverPoint = new Point(-74.0000, 40.7200);
        RedisGeoCommands.GeoLocation<String> geoLocation =
            new RedisGeoCommands.GeoLocation<>(DRIVER_ID.toString(), driverPoint);
        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult =
            new GeoResult<>(geoLocation, new Distance(1.0, Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
            new GeoResults<>(List.of(geoResult));

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.radius(eq("driver:locations"), any(Circle.class),
            any(RedisGeoCommands.GeoRadiusCommandArgs.class))).thenReturn(geoResults);

        matchingService.findMatch(request);

        ArgumentCaptor<MatchResult> resultCaptor = ArgumentCaptor.forClass(MatchResult.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.MATCH_RESULTS),
            eq(DRIVER_ID.toString()), resultCaptor.capture());
        assertTrue(resultCaptor.getValue().matched());
    }

    // ── findMatch with no drivers ──

    @Test
    void findMatch_noDrivers_nullResults_returnsUnmatched() {
        MatchRequest request = createMatchRequest();

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.radius(eq("driver:locations"), any(Circle.class),
            any(RedisGeoCommands.GeoRadiusCommandArgs.class))).thenReturn(null);

        MatchResult result = matchingService.findMatch(request);

        assertFalse(result.matched());
        assertNull(result.driverId());
        assertEquals("No available drivers nearby", result.failureReason());
        assertEquals(TRIP_ID, result.tripId());
        assertEquals(RIDER_ID, result.riderId());
    }

    @Test
    void findMatch_noDrivers_emptyResults_returnsUnmatched() {
        MatchRequest request = createMatchRequest();

        GeoResults<RedisGeoCommands.GeoLocation<String>> emptyResults =
            new GeoResults<>(Collections.emptyList());

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.radius(eq("driver:locations"), any(Circle.class),
            any(RedisGeoCommands.GeoRadiusCommandArgs.class))).thenReturn(emptyResults);

        MatchResult result = matchingService.findMatch(request);

        assertFalse(result.matched());
        assertNull(result.driverId());
        assertEquals("No available drivers nearby", result.failureReason());
    }

    @Test
    void findMatch_noDrivers_sendsKafkaWithRiderIdKey() {
        MatchRequest request = createMatchRequest();

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.radius(eq("driver:locations"), any(Circle.class),
            any(RedisGeoCommands.GeoRadiusCommandArgs.class))).thenReturn(null);

        matchingService.findMatch(request);

        verify(kafkaTemplate).send(eq(KafkaTopics.MATCH_RESULTS),
            eq(RIDER_ID.toString()), any(MatchResult.class));
    }

    @Test
    void findMatch_noDrivers_returnsZeroEtaAndDistance() {
        MatchRequest request = createMatchRequest();

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.radius(eq("driver:locations"), any(Circle.class),
            any(RedisGeoCommands.GeoRadiusCommandArgs.class))).thenReturn(null);

        MatchResult result = matchingService.findMatch(request);

        assertEquals(0, result.etaSeconds());
        assertEquals(0.0, result.distanceKm(), 0.001);
    }

    @Test
    void findMatch_multipleDrivers_selectsNearest() {
        MatchRequest request = createMatchRequest();

        // First driver (nearest)
        Point nearPoint = new Point(-74.0010, 40.7130);
        RedisGeoCommands.GeoLocation<String> nearLoc =
            new RedisGeoCommands.GeoLocation<>(DRIVER_ID.toString(), nearPoint);
        GeoResult<RedisGeoCommands.GeoLocation<String>> nearResult =
            new GeoResult<>(nearLoc, new Distance(0.5, Metrics.KILOMETERS));

        // Second driver (farther)
        UUID farDriverId = UUID.randomUUID();
        Point farPoint = new Point(-74.0200, 40.7300);
        RedisGeoCommands.GeoLocation<String> farLoc =
            new RedisGeoCommands.GeoLocation<>(farDriverId.toString(), farPoint);
        GeoResult<RedisGeoCommands.GeoLocation<String>> farGeoResult =
            new GeoResult<>(farLoc, new Distance(2.0, Metrics.KILOMETERS));

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
            new GeoResults<>(List.of(nearResult, farGeoResult));

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.radius(eq("driver:locations"), any(Circle.class),
            any(RedisGeoCommands.GeoRadiusCommandArgs.class))).thenReturn(geoResults);

        MatchResult result = matchingService.findMatch(request);

        // Should pick the first (nearest) driver since results are sorted ascending
        assertEquals(DRIVER_ID, result.driverId());
        assertEquals(0.5, result.distanceKm(), 0.001);
    }
}
