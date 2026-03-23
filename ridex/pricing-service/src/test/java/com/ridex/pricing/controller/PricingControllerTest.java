package com.ridex.pricing.controller;

import com.ridex.pricing.service.SurgeCalculator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PricingController.class)
class PricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SurgeCalculator surgeCalculator;

    // ---------------------------------------------------------------
    // GET /api/pricing/estimate
    // ---------------------------------------------------------------

    @Test
    void getEstimate_validParams_returnsOkWithPriceEstimate() throws Exception {
        var estimate = new SurgeCalculator.PriceEstimate(2075, 1.0, 200, 1500, 375);
        when(surgeCalculator.estimatePrice(10.0, 15, "abc123")).thenReturn(estimate);

        mockMvc.perform(get("/api/pricing/estimate")
                        .param("distanceKm", "10.0")
                        .param("durationMinutes", "15")
                        .param("geohashCell", "abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCents").value(2075))
                .andExpect(jsonPath("$.data.surgeMultiplier").value(1.0))
                .andExpect(jsonPath("$.data.baseFareCents").value(200))
                .andExpect(jsonPath("$.data.distanceFareCents").value(1500))
                .andExpect(jsonPath("$.data.timeFareCents").value(375));
    }

    @Test
    void getEstimate_defaultGeohash_usesDefault() throws Exception {
        var estimate = new SurgeCalculator.PriceEstimate(500, 1.0, 200, 150, 150);
        when(surgeCalculator.estimatePrice(1.0, 6, "default")).thenReturn(estimate);

        mockMvc.perform(get("/api/pricing/estimate")
                        .param("distanceKm", "1.0")
                        .param("durationMinutes", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCents").value(500));
    }

    @Test
    void getEstimate_withSurge_returnsSurgedPrice() throws Exception {
        var estimate = new SurgeCalculator.PriceEstimate(4800, 2.4, 200, 1500, 300);
        when(surgeCalculator.estimatePrice(10.0, 12, "hotzone")).thenReturn(estimate);

        mockMvc.perform(get("/api/pricing/estimate")
                        .param("distanceKm", "10.0")
                        .param("durationMinutes", "12")
                        .param("geohashCell", "hotzone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCents").value(4800))
                .andExpect(jsonPath("$.data.surgeMultiplier").value(2.4));
    }

    @Test
    void getEstimate_zeroDistance_returnsValidResponse() throws Exception {
        var estimate = new SurgeCalculator.PriceEstimate(325, 1.0, 200, 0, 125);
        when(surgeCalculator.estimatePrice(0.0, 5, "default")).thenReturn(estimate);

        mockMvc.perform(get("/api/pricing/estimate")
                        .param("distanceKm", "0.0")
                        .param("durationMinutes", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.distanceFareCents").value(0))
                .andExpect(jsonPath("$.data.totalCents").value(325));
    }

    @Test
    void getEstimate_missingDistanceKm_returns400() throws Exception {
        mockMvc.perform(get("/api/pricing/estimate")
                        .param("durationMinutes", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEstimate_missingDurationMinutes_returns400() throws Exception {
        mockMvc.perform(get("/api/pricing/estimate")
                        .param("distanceKm", "5.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEstimate_missingAllRequiredParams_returns400() throws Exception {
        mockMvc.perform(get("/api/pricing/estimate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEstimate_invalidDistanceKmFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/pricing/estimate")
                        .param("distanceKm", "not-a-number")
                        .param("durationMinutes", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEstimate_invalidDurationFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/pricing/estimate")
                        .param("distanceKm", "5.0")
                        .param("durationMinutes", "abc"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // GET /api/pricing/surge
    // ---------------------------------------------------------------

    @Test
    void getSurge_withGeohash_returnsMultiplier() throws Exception {
        when(surgeCalculator.getSurgeMultiplier("downtown")).thenReturn(1.8);

        mockMvc.perform(get("/api/pricing/surge")
                        .param("geohashCell", "downtown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.geohashCell").value("downtown"))
                .andExpect(jsonPath("$.data.surgeMultiplier").value(1.8));
    }

    @Test
    void getSurge_defaultGeohash_usesDefault() throws Exception {
        when(surgeCalculator.getSurgeMultiplier("default")).thenReturn(1.0);

        mockMvc.perform(get("/api/pricing/surge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.geohashCell").value("default"))
                .andExpect(jsonPath("$.data.surgeMultiplier").value(1.0));
    }

    @Test
    void getSurge_highSurge_returnsExactValue() throws Exception {
        when(surgeCalculator.getSurgeMultiplier("surge-zone")).thenReturn(3.0);

        mockMvc.perform(get("/api/pricing/surge")
                        .param("geohashCell", "surge-zone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.surgeMultiplier").value(3.0));
    }

    @Test
    void getSurge_noSurge_returns1() throws Exception {
        when(surgeCalculator.getSurgeMultiplier("quiet-area")).thenReturn(1.0);

        mockMvc.perform(get("/api/pricing/surge")
                        .param("geohashCell", "quiet-area"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.surgeMultiplier").value(1.0));
    }

    @Test
    void getSurge_responseIncludesTimestamp() throws Exception {
        when(surgeCalculator.getSurgeMultiplier("default")).thenReturn(1.0);

        mockMvc.perform(get("/api/pricing/surge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
