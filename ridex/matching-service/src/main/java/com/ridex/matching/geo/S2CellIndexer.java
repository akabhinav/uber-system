package com.ridex.matching.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class S2CellIndexer {
    private static final Logger log = LoggerFactory.getLogger(S2CellIndexer.class);

    @Value("${ridex.matching.s2-cell-level:13}")
    private int cellLevel;

    public String getCellId(double lat, double lng) {
        // S2 cell computation - simplified for local dev
        // In production, use com.google.common.geometry.S2CellId
        long latCell = (long) ((lat + 90) * Math.pow(2, cellLevel));
        long lngCell = (long) ((lng + 180) * Math.pow(2, cellLevel));
        return String.format("s2_%d_%d_%d", cellLevel, latCell, lngCell);
    }

    public boolean isWithinRadius(double lat1, double lng1, double lat2, double lng2, double radiusKm) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = 6371 * c;
        return distanceKm <= radiusKm;
    }
}
