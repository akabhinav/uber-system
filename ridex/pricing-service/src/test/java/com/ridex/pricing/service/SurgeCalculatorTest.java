package com.ridex.pricing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurgeCalculatorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SurgeCalculator surgeCalculator;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        surgeCalculator = new SurgeCalculator(redisTemplate);
        ReflectionTestUtils.setField(surgeCalculator, "baseFareCents", 200);
        ReflectionTestUtils.setField(surgeCalculator, "perKmCents", 150);
        ReflectionTestUtils.setField(surgeCalculator, "perMinuteCents", 25);
    }

    // ---------------------------------------------------------------
    // getSurgeMultiplier tests
    // ---------------------------------------------------------------

    @Test
    void getSurgeMultiplier_cachedValue_returnsCachedSurge() {
        when(valueOperations.get("surge:abc123")).thenReturn("1.75");

        double surge = surgeCalculator.getSurgeMultiplier("abc123");

        assertEquals(1.75, surge);
        // Should not look up demand/supply when cache hit
        verify(valueOperations, never()).get("demand:abc123");
        verify(valueOperations, never()).get("supply:abc123");
    }

    @Test
    void getSurgeMultiplier_noCache_zeroSupply_returnsMaxDefault() {
        when(valueOperations.get("surge:cell1")).thenReturn(null);
        when(valueOperations.get("demand:cell1")).thenReturn("10");
        when(valueOperations.get("supply:cell1")).thenReturn("0");

        double surge = surgeCalculator.getSurgeMultiplier("cell1");

        assertEquals(2.0, surge);
    }

    @Test
    void getSurgeMultiplier_noCache_nullSupply_returnsMaxDefault() {
        when(valueOperations.get("surge:cell1")).thenReturn(null);
        when(valueOperations.get("demand:cell1")).thenReturn("5");
        when(valueOperations.get("supply:cell1")).thenReturn(null);

        // supply key missing -> getLongValue returns 0 -> supply==0 -> returns 2.0
        double surge = surgeCalculator.getSurgeMultiplier("cell1");

        assertEquals(2.0, surge);
    }

    @Test
    void getSurgeMultiplier_noCache_lowDemand_returnsMinimumOf1() {
        when(valueOperations.get("surge:cell1")).thenReturn(null);
        when(valueOperations.get("demand:cell1")).thenReturn("1");
        when(valueOperations.get("supply:cell1")).thenReturn("10");

        // ratio = 1/10 = 0.1, clamped to min 1.0
        double surge = surgeCalculator.getSurgeMultiplier("cell1");

        assertEquals(1.0, surge);
    }

    @Test
    void getSurgeMultiplier_noCache_highDemand_clampedAt3() {
        when(valueOperations.get("surge:cell1")).thenReturn(null);
        when(valueOperations.get("demand:cell1")).thenReturn("100");
        when(valueOperations.get("supply:cell1")).thenReturn("10");

        // ratio = 10.0, clamped to max 3.0
        double surge = surgeCalculator.getSurgeMultiplier("cell1");

        assertEquals(3.0, surge);
    }

    @Test
    void getSurgeMultiplier_noCache_equalDemandAndSupply_returns1() {
        when(valueOperations.get("surge:cell1")).thenReturn(null);
        when(valueOperations.get("demand:cell1")).thenReturn("5");
        when(valueOperations.get("supply:cell1")).thenReturn("5");

        double surge = surgeCalculator.getSurgeMultiplier("cell1");

        assertEquals(1.0, surge);
    }

    @Test
    void getSurgeMultiplier_noCache_moderateRatio_roundedToTwoDecimals() {
        when(valueOperations.get("surge:cell1")).thenReturn(null);
        when(valueOperations.get("demand:cell1")).thenReturn("7");
        when(valueOperations.get("supply:cell1")).thenReturn("3");

        // ratio = 7/3 = 2.3333..., rounded to 2.33
        double surge = surgeCalculator.getSurgeMultiplier("cell1");

        assertEquals(2.33, surge);
    }

    @Test
    void getSurgeMultiplier_noCache_zeroDemand_returns1() {
        when(valueOperations.get("surge:cell1")).thenReturn(null);
        when(valueOperations.get("demand:cell1")).thenReturn("0");
        when(valueOperations.get("supply:cell1")).thenReturn("5");

        // ratio = 0.0, clamped to min 1.0
        double surge = surgeCalculator.getSurgeMultiplier("cell1");

        assertEquals(1.0, surge);
    }

    @Test
    void getSurgeMultiplier_noCache_bothKeysNull_returnsMaxDefault() {
        when(valueOperations.get("surge:cell1")).thenReturn(null);
        when(valueOperations.get("demand:cell1")).thenReturn(null);
        when(valueOperations.get("supply:cell1")).thenReturn(null);

        // demand=0, supply=0 -> supply==0 branch -> returns 2.0
        double surge = surgeCalculator.getSurgeMultiplier("cell1");

        assertEquals(2.0, surge);
    }

    // ---------------------------------------------------------------
    // estimatePrice tests
    // ---------------------------------------------------------------

    @Test
    void estimatePrice_noSurge_computesCorrectBreakdown() {
        // Set surge cache to 1.0 (no surge)
        when(valueOperations.get("surge:cell1")).thenReturn("1.0");

        // 10 km, 15 minutes
        // base=200, distance=10*150=1500, time=15*25=375, subtotal=2075, surge=1.0 -> total=2075
        SurgeCalculator.PriceEstimate estimate = surgeCalculator.estimatePrice(10.0, 15, "cell1");

        assertEquals(2075, estimate.totalCents());
        assertEquals(1.0, estimate.surgeMultiplier());
        assertEquals(200, estimate.baseFareCents());
        assertEquals(1500, estimate.distanceFareCents());
        assertEquals(375, estimate.timeFareCents());
    }

    @Test
    void estimatePrice_withSurge_multipliesSubtotal() {
        when(valueOperations.get("surge:cell2")).thenReturn("2.0");

        // 5 km, 10 minutes
        // base=200, distance=5*150=750, time=10*25=250, subtotal=1200, surge=2.0 -> total=2400
        SurgeCalculator.PriceEstimate estimate = surgeCalculator.estimatePrice(5.0, 10, "cell2");

        assertEquals(2400, estimate.totalCents());
        assertEquals(2.0, estimate.surgeMultiplier());
        assertEquals(200, estimate.baseFareCents());
        assertEquals(750, estimate.distanceFareCents());
        assertEquals(250, estimate.timeFareCents());
    }

    @Test
    void estimatePrice_zeroDistance_chargesBaseAndTimeOnly() {
        when(valueOperations.get("surge:cell1")).thenReturn("1.0");

        // 0 km, 5 minutes
        // base=200, distance=0, time=5*25=125, subtotal=325, surge=1.0 -> total=325
        SurgeCalculator.PriceEstimate estimate = surgeCalculator.estimatePrice(0.0, 5, "cell1");

        assertEquals(325, estimate.totalCents());
        assertEquals(0, estimate.distanceFareCents());
    }

    @Test
    void estimatePrice_zeroDuration_chargesBaseAndDistanceOnly() {
        when(valueOperations.get("surge:cell1")).thenReturn("1.0");

        // 8 km, 0 minutes
        // base=200, distance=8*150=1200, time=0, subtotal=1400
        SurgeCalculator.PriceEstimate estimate = surgeCalculator.estimatePrice(8.0, 0, "cell1");

        assertEquals(1400, estimate.totalCents());
        assertEquals(0, estimate.timeFareCents());
    }

    @Test
    void estimatePrice_zeroDistanceAndDuration_chargesBaseOnly() {
        when(valueOperations.get("surge:cell1")).thenReturn("1.0");

        SurgeCalculator.PriceEstimate estimate = surgeCalculator.estimatePrice(0.0, 0, "cell1");

        assertEquals(200, estimate.totalCents());
        assertEquals(200, estimate.baseFareCents());
        assertEquals(0, estimate.distanceFareCents());
        assertEquals(0, estimate.timeFareCents());
    }

    @Test
    void estimatePrice_maxSurge_correctMultiplication() {
        when(valueOperations.get("surge:cell1")).thenReturn("3.0");

        // 10 km, 20 min
        // base=200, distance=1500, time=500, subtotal=2200, surge=3.0 -> total=6600
        SurgeCalculator.PriceEstimate estimate = surgeCalculator.estimatePrice(10.0, 20, "cell1");

        assertEquals(6600, estimate.totalCents());
        assertEquals(3.0, estimate.surgeMultiplier());
    }

    @Test
    void estimatePrice_fractionalDistance_truncatesToInt() {
        when(valueOperations.get("surge:cell1")).thenReturn("1.0");

        // 3.7 km, 10 min
        // distance = (int)(3.7 * 150) = (int)(555.0) = 555
        // base=200, time=250, subtotal=1005
        SurgeCalculator.PriceEstimate estimate = surgeCalculator.estimatePrice(3.7, 10, "cell1");

        assertEquals(1005, estimate.totalCents());
        assertEquals(555, estimate.distanceFareCents());
    }

    @Test
    void estimatePrice_fractionalSurge_truncatesTotalToInt() {
        when(valueOperations.get("surge:cell1")).thenReturn("1.5");

        // 2 km, 4 min
        // base=200, distance=300, time=100, subtotal=600, surge=1.5 -> (int)(900.0)=900
        SurgeCalculator.PriceEstimate estimate = surgeCalculator.estimatePrice(2.0, 4, "cell1");

        assertEquals(900, estimate.totalCents());
        assertEquals(1.5, estimate.surgeMultiplier());
    }

    @Test
    void estimatePrice_veryLongTrip_handlesLargeValues() {
        when(valueOperations.get("surge:cell1")).thenReturn("1.0");

        // 100 km, 120 minutes
        // base=200, distance=100*150=15000, time=120*25=3000, subtotal=18200
        SurgeCalculator.PriceEstimate estimate = surgeCalculator.estimatePrice(100.0, 120, "cell1");

        assertEquals(18200, estimate.totalCents());
    }

    @Test
    void estimatePrice_surgeFromDemandSupply_integratesCorrectly() {
        // No cached surge, so it computes from demand/supply
        when(valueOperations.get("surge:cell1")).thenReturn(null);
        when(valueOperations.get("demand:cell1")).thenReturn("10");
        when(valueOperations.get("supply:cell1")).thenReturn("5");

        // ratio = 2.0, surge = 2.0
        // 10 km, 10 min: base=200, distance=1500, time=250, subtotal=1950, total=3900
        SurgeCalculator.PriceEstimate estimate = surgeCalculator.estimatePrice(10.0, 10, "cell1");

        assertEquals(3900, estimate.totalCents());
        assertEquals(2.0, estimate.surgeMultiplier());
    }

    @Test
    void estimatePrice_negativeDistance_producesNegativeDistanceFare() {
        when(valueOperations.get("surge:cell1")).thenReturn("1.0");

        // Negative distance is not validated - the math just runs
        // distance = (int)(-5.0 * 150) = -750
        // base=200, time=5*25=125, subtotal=200+(-750)+125=-425, total=-425
        SurgeCalculator.PriceEstimate estimate = surgeCalculator.estimatePrice(-5.0, 5, "cell1");

        assertTrue(estimate.totalCents() < 0,
                "Negative distance produces a negative total since there is no input validation");
        assertEquals(-750, estimate.distanceFareCents());
    }
}
