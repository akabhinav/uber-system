package com.ridex.matching.service;

import com.ridex.commons.config.KafkaTopics;
import com.ridex.commons.events.MatchRequest;
import com.ridex.commons.events.MatchResult;
import com.ridex.commons.exception.DriverUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MatchingService {
    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, MatchResult> kafkaTemplate;

    @Value("${ridex.matching.search-radius-km:5}")
    private double searchRadiusKm;

    @Value("${ridex.matching.max-candidates:10}")
    private int maxCandidates;

    public MatchingService(StringRedisTemplate redisTemplate,
                           KafkaTemplate<String, MatchResult> kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    public MatchResult findMatch(MatchRequest request) {
        log.info("Finding match for trip={}, rider={}", request.tripId(), request.riderId());

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
            .radius("driver:locations",
                new Circle(new Point(request.pickupLng(), request.pickupLat()),
                    new Distance(searchRadiusKm, Metrics.KILOMETERS)),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                    .includeCoordinates()
                    .includeDistance()
                    .sortAscending()
                    .limit(maxCandidates));

        if (results == null || results.getContent().isEmpty()) {
            log.warn("No drivers found near pickup for trip={}", request.tripId());
            MatchResult noMatch = new MatchResult(
                request.tripId(), request.riderId(), null,
                0, 0, 0, 0, false,
                "No available drivers nearby", Instant.now());
            kafkaTemplate.send(KafkaTopics.MATCH_RESULTS, request.riderId().toString(), noMatch);
            return noMatch;
        }

        GeoResult<RedisGeoCommands.GeoLocation<String>> nearest = results.getContent().get(0);
        String driverIdStr = nearest.getContent().getName();
        UUID driverId = UUID.fromString(driverIdStr);
        Point driverPoint = nearest.getContent().getPoint();
        double distanceKm = nearest.getDistance().getValue();
        int etaSeconds = (int) (distanceKm / 0.5 * 60); // rough estimate: 30km/h avg

        log.info("Matched trip={} with driver={}, distance={}km, eta={}s",
            request.tripId(), driverId, distanceKm, etaSeconds);

        MatchResult matchResult = new MatchResult(
            request.tripId(), request.riderId(), driverId,
            driverPoint.getY(), driverPoint.getX(),
            etaSeconds, distanceKm, true, null, Instant.now());

        kafkaTemplate.send(KafkaTopics.MATCH_RESULTS, driverId.toString(), matchResult);
        return matchResult;
    }
}
