package com.ridex.matching.geo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class H3DemandGridTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private H3DemandGrid demandGrid;

    @BeforeEach
    void setUp() throws Exception {
        demandGrid = new H3DemandGrid(redisTemplate);
        // Trigger H3Core initialization
        demandGrid.init();
    }

    // ── getCellId tests ──

    @Test
    void getCellId_validCoordinates_returnsNonNullCellId() {
        String cellId = demandGrid.getCellId(40.7128, -74.0060);

        assertNotNull(cellId);
        assertNotEquals("unknown", cellId);
    }

    @Test
    void getCellId_sameLocation_returnsSameCellId() {
        String cellId1 = demandGrid.getCellId(40.7128, -74.0060);
        String cellId2 = demandGrid.getCellId(40.7128, -74.0060);

        assertEquals(cellId1, cellId2);
    }

    @Test
    void getCellId_differentLocations_returnsDifferentCellIds() {
        // Two far-apart locations should produce different cell ids
        String cellNY = demandGrid.getCellId(40.7128, -74.0060);
        String cellLA = demandGrid.getCellId(34.0522, -118.2437);

        assertNotEquals(cellNY, cellLA);
    }

    @Test
    void getCellId_h3Null_returnsUnknown() throws Exception {
        // Set h3 field to null to simulate init failure
        Field h3Field = H3DemandGrid.class.getDeclaredField("h3");
        h3Field.setAccessible(true);
        h3Field.set(demandGrid, null);

        String cellId = demandGrid.getCellId(40.7128, -74.0060);

        assertEquals("unknown", cellId);
    }

    // ── incrementDemand tests ──

    @Test
    void incrementDemand_callsRedisIncrement() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        demandGrid.incrementDemand(40.7128, -74.0060);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).increment(argThat(key -> key.startsWith("demand:")));
    }

    @Test
    void incrementDemand_usesCorrectCellIdInKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String expectedCellId = demandGrid.getCellId(40.7128, -74.0060);
        demandGrid.incrementDemand(40.7128, -74.0060);

        verify(valueOperations).increment("demand:" + expectedCellId);
    }

    // ── getDemand tests ──

    @Test
    void getDemand_existingValue_returnsParsedLong() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String cellId = demandGrid.getCellId(40.7128, -74.0060);
        when(valueOperations.get("demand:" + cellId)).thenReturn("42");

        long demand = demandGrid.getDemand(40.7128, -74.0060);

        assertEquals(42L, demand);
    }

    @Test
    void getDemand_nullValue_returnsZero() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String cellId = demandGrid.getCellId(40.7128, -74.0060);
        when(valueOperations.get("demand:" + cellId)).thenReturn(null);

        long demand = demandGrid.getDemand(40.7128, -74.0060);

        assertEquals(0L, demand);
    }

    @Test
    void getDemand_largeValue_parsesCorrectly() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String cellId = demandGrid.getCellId(40.7128, -74.0060);
        when(valueOperations.get("demand:" + cellId)).thenReturn("999999");

        long demand = demandGrid.getDemand(40.7128, -74.0060);

        assertEquals(999999L, demand);
    }
}
