package com.ridex.matching.geo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class S2CellIndexerTest {

    private S2CellIndexer indexer;

    @BeforeEach
    void setUp() throws Exception {
        indexer = new S2CellIndexer();
        // Set the @Value field via reflection
        Field cellLevelField = S2CellIndexer.class.getDeclaredField("cellLevel");
        cellLevelField.setAccessible(true);
        cellLevelField.set(indexer, 13);
    }

    // ── getCellId tests ──

    @Test
    void getCellId_returnsFormattedString() {
        String cellId = indexer.getCellId(40.7128, -74.0060);

        assertNotNull(cellId);
        assertTrue(cellId.startsWith("s2_13_"));
    }

    @Test
    void getCellId_sameCoordinates_returnsSameCellId() {
        String cellId1 = indexer.getCellId(40.7128, -74.0060);
        String cellId2 = indexer.getCellId(40.7128, -74.0060);

        assertEquals(cellId1, cellId2);
    }

    @Test
    void getCellId_differentCoordinates_returnsDifferentCellIds() {
        String cellNY = indexer.getCellId(40.7128, -74.0060);
        String cellLA = indexer.getCellId(34.0522, -118.2437);

        assertNotEquals(cellNY, cellLA);
    }

    @Test
    void getCellId_containsCellLevel() {
        String cellId = indexer.getCellId(51.5074, -0.1278);

        // Format: s2_{level}_{latCell}_{lngCell}
        String[] parts = cellId.split("_");
        assertEquals(4, parts.length);
        assertEquals("s2", parts[0]);
        assertEquals("13", parts[1]);
    }

    @Test
    void getCellId_equator_zeroMeridian() {
        String cellId = indexer.getCellId(0.0, 0.0);

        assertNotNull(cellId);
        assertTrue(cellId.startsWith("s2_13_"));
        // lat=0 -> (0+90)*2^13 = 90*8192 = 737280
        // lng=0 -> (0+180)*2^13 = 180*8192 = 1474560
        assertEquals("s2_13_737280_1474560", cellId);
    }

    @Test
    void getCellId_negativeBothCoordinates() {
        // Buenos Aires area: -34.6, -58.4
        String cellId = indexer.getCellId(-34.6037, -58.3816);

        assertNotNull(cellId);
        assertTrue(cellId.startsWith("s2_13_"));
    }

    @Test
    void getCellId_differentCellLevel() throws Exception {
        Field cellLevelField = S2CellIndexer.class.getDeclaredField("cellLevel");
        cellLevelField.setAccessible(true);
        cellLevelField.set(indexer, 10);

        String cellId = indexer.getCellId(40.7128, -74.0060);

        assertTrue(cellId.startsWith("s2_10_"));
    }

    // ── isWithinRadius tests ──

    @Test
    void isWithinRadius_samePoint_returnsTrue() {
        boolean result = indexer.isWithinRadius(40.7128, -74.0060, 40.7128, -74.0060, 1.0);

        assertTrue(result);
    }

    @Test
    void isWithinRadius_nearbyPoints_withinRadius_returnsTrue() {
        // Two points in Manhattan roughly 1 km apart
        boolean result = indexer.isWithinRadius(40.7128, -74.0060, 40.7218, -74.0060, 2.0);

        assertTrue(result);
    }

    @Test
    void isWithinRadius_farPoints_outsideRadius_returnsFalse() {
        // NY to LA, clearly outside 100 km
        boolean result = indexer.isWithinRadius(40.7128, -74.0060, 34.0522, -118.2437, 100.0);

        assertFalse(result);
    }

    @Test
    void isWithinRadius_exactlyAtBoundary() {
        // Points roughly 1.11 km apart (0.01 degrees latitude at equator ~1.11 km)
        // Testing with radius slightly above the distance
        boolean within = indexer.isWithinRadius(0.0, 0.0, 0.01, 0.0, 1.2);
        boolean outside = indexer.isWithinRadius(0.0, 0.0, 0.01, 0.0, 1.0);

        assertTrue(within);
        assertFalse(outside);
    }

    @Test
    void isWithinRadius_symmetricCheck() {
        double lat1 = 48.8566, lng1 = 2.3522;
        double lat2 = 48.8700, lng2 = 2.3600;
        double radius = 5.0;

        boolean forward = indexer.isWithinRadius(lat1, lng1, lat2, lng2, radius);
        boolean reverse = indexer.isWithinRadius(lat2, lng2, lat1, lng1, radius);

        assertEquals(forward, reverse);
    }

    @Test
    void isWithinRadius_zeroRadius_samePoint_returnsTrue() {
        boolean result = indexer.isWithinRadius(40.7128, -74.0060, 40.7128, -74.0060, 0.0);

        assertTrue(result);
    }

    @Test
    void isWithinRadius_zeroRadius_differentPoints_returnsFalse() {
        boolean result = indexer.isWithinRadius(40.7128, -74.0060, 40.7129, -74.0060, 0.0);

        assertFalse(result);
    }

    @Test
    void isWithinRadius_crossEquator() {
        // Points on either side of the equator, close together
        boolean result = indexer.isWithinRadius(0.005, 0.0, -0.005, 0.0, 2.0);

        assertTrue(result);
    }

    @Test
    void isWithinRadius_largeRadius_alwaysTrue() {
        // Radius larger than earth's circumference
        boolean result = indexer.isWithinRadius(90.0, 0.0, -90.0, 0.0, 25000.0);

        assertTrue(result);
    }
}
