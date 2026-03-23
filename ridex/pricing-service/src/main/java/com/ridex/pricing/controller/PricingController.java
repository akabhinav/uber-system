package com.ridex.pricing.controller;

import com.ridex.commons.dto.ApiResponse;
import com.ridex.pricing.service.SurgeCalculator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {
    private final SurgeCalculator surgeCalculator;

    public PricingController(SurgeCalculator surgeCalculator) {
        this.surgeCalculator = surgeCalculator;
    }

    @GetMapping("/estimate")
    public ApiResponse<SurgeCalculator.PriceEstimate> getEstimate(
            @RequestParam double distanceKm,
            @RequestParam int durationMinutes,
            @RequestParam(defaultValue = "default") String geohashCell) {
        return ApiResponse.ok(surgeCalculator.estimatePrice(distanceKm, durationMinutes, geohashCell));
    }

    @GetMapping("/surge")
    public ApiResponse<SurgeInfo> getSurge(
            @RequestParam(defaultValue = "default") String geohashCell) {
        double surge = surgeCalculator.getSurgeMultiplier(geohashCell);
        return ApiResponse.ok(new SurgeInfo(geohashCell, surge));
    }

    public record SurgeInfo(String geohashCell, double surgeMultiplier) {}
}
