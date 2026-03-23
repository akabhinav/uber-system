package com.ridex.matching.geo;

import com.uber.h3core.H3Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class H3DemandGrid {
    private static final Logger log = LoggerFactory.getLogger(H3DemandGrid.class);
    private H3Core h3;
    private final StringRedisTemplate redisTemplate;

    private static final int H3_RESOLUTION = 8;

    public H3DemandGrid(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            h3 = H3Core.newInstance();
            log.info("H3 initialized at resolution {}", H3_RESOLUTION);
        } catch (Exception e) {
            log.error("Failed to initialize H3", e);
        }
    }

    public String getCellId(double lat, double lng) {
        if (h3 == null) return "unknown";
        return h3.latLngToCellAddress(lat, lng, H3_RESOLUTION);
    }

    public void incrementDemand(double lat, double lng) {
        String cellId = getCellId(lat, lng);
        redisTemplate.opsForValue().increment("demand:" + cellId);
    }

    public long getDemand(double lat, double lng) {
        String cellId = getCellId(lat, lng);
        String val = redisTemplate.opsForValue().get("demand:" + cellId);
        return val != null ? Long.parseLong(val) : 0;
    }
}
