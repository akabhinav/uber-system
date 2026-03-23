package com.ridex.matching.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GraphHopperRouter {
    private static final Logger log = LoggerFactory.getLogger(GraphHopperRouter.class);

    // Haversine-based ETA for local dev without OSM data
    public RouteResult calculateRoute(double fromLat, double fromLng, double toLat, double toLng) {
        double distanceKm = haversineDistance(fromLat, fromLng, toLat, toLng);
        double avgSpeedKmh = 30.0; // city average
        int etaSeconds = (int) ((distanceKm / avgSpeedKmh) * 3600);

        log.debug("Route: ({},{}) -> ({},{}) = {}km, {}s",
            fromLat, fromLng, toLat, toLng, distanceKm, etaSeconds);
        return new RouteResult(distanceKm, etaSeconds);
    }

    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }

    public record RouteResult(double distanceKm, int etaSeconds) {}
}
