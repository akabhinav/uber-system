package com.ridex.pricing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SurgeCalculator {
    private static final Logger log = LoggerFactory.getLogger(SurgeCalculator.class);
    private final StringRedisTemplate redisTemplate;

    @Value("${ridex.pricing.base-fare-cents:200}")
    private int baseFareCents;

    @Value("${ridex.pricing.per-km-cents:150}")
    private int perKmCents;

    @Value("${ridex.pricing.per-minute-cents:25}")
    private int perMinuteCents;

    public SurgeCalculator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public double getSurgeMultiplier(String geohashCell) {
        String surgeKey = "surge:" + geohashCell;
        String val = redisTemplate.opsForValue().get(surgeKey);
        if (val != null) {
            return Double.parseDouble(val);
        }

        // Calculate based on demand/supply ratio
        String demandKey = "demand:" + geohashCell;
        String supplyKey = "supply:" + geohashCell;
        long demand = getLongValue(demandKey);
        long supply = getLongValue(supplyKey);

        if (supply == 0) return 2.0; // high surge if no supply
        double ratio = (double) demand / supply;
        double surge = Math.max(1.0, Math.min(3.0, ratio));

        log.debug("Surge for cell={}: demand={}, supply={}, multiplier={}",
            geohashCell, demand, supply, surge);
        return Math.round(surge * 100.0) / 100.0;
    }

    public PriceEstimate estimatePrice(double distanceKm, int durationMinutes, String geohashCell) {
        double surge = getSurgeMultiplier(geohashCell);
        int baseFare = baseFareCents;
        int distanceFare = (int) (distanceKm * perKmCents);
        int timeFare = durationMinutes * perMinuteCents;
        int subtotal = baseFare + distanceFare + timeFare;
        int totalCents = (int) (subtotal * surge);

        log.info("Price estimate: base={}, distance={}, time={}, surge={}, total={}",
            baseFare, distanceFare, timeFare, surge, totalCents);
        return new PriceEstimate(totalCents, surge, baseFare, distanceFare, timeFare);
    }

    private long getLongValue(String key) {
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Long.parseLong(val) : 0;
    }

    public record PriceEstimate(int totalCents, double surgeMultiplier,
                                 int baseFareCents, int distanceFareCents, int timeFareCents) {}
}
