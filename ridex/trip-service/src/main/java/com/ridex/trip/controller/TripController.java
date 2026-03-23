package com.ridex.trip.controller;

import com.ridex.commons.dto.ApiResponse;
import com.ridex.commons.dto.TripDTO;
import com.ridex.trip.service.TripService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public Mono<ApiResponse<TripDTO>> requestTrip(@RequestBody TripRequest request) {
        return tripService.requestTrip(
            request.riderId(), request.pickupLat(), request.pickupLng(),
            request.dropoffLat(), request.dropoffLng(), request.fareEstimateCents()
        ).map(ApiResponse::ok);
    }

    @GetMapping("/{tripId}")
    public Mono<ApiResponse<TripDTO>> getTrip(@PathVariable UUID tripId) {
        return tripService.getTrip(tripId).map(ApiResponse::ok);
    }

    @GetMapping("/rider/{riderId}")
    public Mono<ApiResponse<List<TripDTO>>> getRiderTrips(@PathVariable UUID riderId) {
        return tripService.getTripsByRider(riderId)
            .collectList()
            .map(ApiResponse::ok);
    }

    @PatchMapping("/{tripId}/status")
    public Mono<ApiResponse<TripDTO>> updateStatus(@PathVariable UUID tripId,
                                                     @RequestBody StatusUpdate update) {
        return tripService.updateTripStatus(tripId, update.status()).map(ApiResponse::ok);
    }

    @PostMapping("/{tripId}/cancel")
    public Mono<ApiResponse<TripDTO>> cancelTrip(@PathVariable UUID tripId) {
        return tripService.cancelTrip(tripId).map(ApiResponse::ok);
    }

    public record TripRequest(UUID riderId, double pickupLat, double pickupLng,
                               double dropoffLat, double dropoffLng, int fareEstimateCents) {}
    public record StatusUpdate(String status) {}
}
