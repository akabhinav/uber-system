package com.ridex.matching.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphHopperRouterTest {

    private GraphHopperRouter router;

    @BeforeEach
    void setUp() {
        router = new GraphHopperRouter();
    }

    // ── calculateRoute tests ──

    @Test
    void calculateRoute_knownPair_returnsExpectedDistanceAndEta() {
        // New York (40.7128, -74.0060) -> Newark (40.7357, -74.1724)
        // Approximate great-circle distance ~14 km
        GraphHopperRouter.RouteResult result =
            router.calculateRoute(40.7128, -74.0060, 40.7357, -74.1724);

        assertEquals(14.0, result.distanceKm(), 1.5);
        // ETA at 30 km/h: ~14/30*3600 ~= 1680s
        assertTrue(result.etaSeconds() > 0);
        int expectedEta = (int) ((result.distanceKm() / 30.0) * 3600);
        assertEquals(expectedEta, result.etaSeconds());
    }

    @Test
    void calculateRoute_samePoint_returnsZero() {
        GraphHopperRouter.RouteResult result =
            router.calculateRoute(51.5074, -0.1278, 51.5074, -0.1278);

        assertEquals(0.0, result.distanceKm(), 0.001);
        assertEquals(0, result.etaSeconds());
    }

    @Test
    void calculateRoute_longDistance_losAngelesToNewYork() {
        // LA (34.0522, -118.2437) -> NY (40.7128, -74.0060)
        // Great-circle distance ~3944 km
        GraphHopperRouter.RouteResult result =
            router.calculateRoute(34.0522, -118.2437, 40.7128, -74.0060);

        assertEquals(3944.0, result.distanceKm(), 50.0);
        assertTrue(result.etaSeconds() > 0);
    }

    @Test
    void calculateRoute_shortDistance_nearbyPoints() {
        // Two points ~1.1 km apart in central London
        GraphHopperRouter.RouteResult result =
            router.calculateRoute(51.5074, -0.1278, 51.5174, -0.1278);

        assertEquals(1.11, result.distanceKm(), 0.2);
        // At 30 km/h this should be a short ETA
        assertTrue(result.etaSeconds() < 300);
    }

    @Test
    void calculateRoute_crossEquator() {
        // Quito (0.1807, -78.4678) -> Bogota (4.7110, -74.0721)
        GraphHopperRouter.RouteResult result =
            router.calculateRoute(0.1807, -78.4678, 4.7110, -74.0721);

        assertTrue(result.distanceKm() > 0);
        assertTrue(result.etaSeconds() > 0);
    }

    @Test
    void calculateRoute_etaConsistentWithDistance() {
        GraphHopperRouter.RouteResult result =
            router.calculateRoute(48.8566, 2.3522, 48.8700, 2.3400);

        // Verify ETA = (distance / 30) * 3600
        int expectedEta = (int) ((result.distanceKm() / 30.0) * 3600);
        assertEquals(expectedEta, result.etaSeconds());
    }

    // ── haversineDistance tests (via calculateRoute since haversine is private) ──

    @Test
    void haversine_symmetricDistance() {
        // A -> B should equal B -> A
        GraphHopperRouter.RouteResult forward =
            router.calculateRoute(40.7128, -74.0060, 34.0522, -118.2437);
        GraphHopperRouter.RouteResult reverse =
            router.calculateRoute(34.0522, -118.2437, 40.7128, -74.0060);

        assertEquals(forward.distanceKm(), reverse.distanceKm(), 0.001);
    }

    @Test
    void haversine_antipodal_returnsMaxDistance() {
        // Opposite sides of the earth: (0,0) -> (0,180)
        // Should be approximately half the earth's circumference ~20015 km
        GraphHopperRouter.RouteResult result =
            router.calculateRoute(0.0, 0.0, 0.0, 180.0);

        assertEquals(20015.0, result.distanceKm(), 100.0);
    }

    @Test
    void haversine_poleToEquator() {
        // North pole (90,0) -> equator (0,0) ~10008 km
        GraphHopperRouter.RouteResult result =
            router.calculateRoute(90.0, 0.0, 0.0, 0.0);

        assertEquals(10008.0, result.distanceKm(), 50.0);
    }
}
