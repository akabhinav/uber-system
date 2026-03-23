package com.ridex.trip.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.Instant;
import java.util.UUID;

@Table("trips")
public class Trip {
    @Id
    private UUID id;
    @Column("rider_id")
    private UUID riderId;
    @Column("driver_id")
    private UUID driverId;
    private String status;
    @Column("pickup_lat")
    private Double pickupLat;
    @Column("pickup_lng")
    private Double pickupLng;
    @Column("dropoff_lat")
    private Double dropoffLat;
    @Column("dropoff_lng")
    private Double dropoffLng;
    @Column("fare_cents")
    private Integer fareCents;
    @Column("surge_multiplier")
    private Double surgeMultiplier;
    @Column("started_at")
    private Instant startedAt;
    @Column("completed_at")
    private Instant completedAt;
    @Column("created_at")
    private Instant createdAt;

    public Trip() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRiderId() { return riderId; }
    public void setRiderId(UUID riderId) { this.riderId = riderId; }
    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getPickupLat() { return pickupLat; }
    public void setPickupLat(Double pickupLat) { this.pickupLat = pickupLat; }
    public Double getPickupLng() { return pickupLng; }
    public void setPickupLng(Double pickupLng) { this.pickupLng = pickupLng; }
    public Double getDropoffLat() { return dropoffLat; }
    public void setDropoffLat(Double dropoffLat) { this.dropoffLat = dropoffLat; }
    public Double getDropoffLng() { return dropoffLng; }
    public void setDropoffLng(Double dropoffLng) { this.dropoffLng = dropoffLng; }
    public Integer getFareCents() { return fareCents; }
    public void setFareCents(Integer fareCents) { this.fareCents = fareCents; }
    public Double getSurgeMultiplier() { return surgeMultiplier; }
    public void setSurgeMultiplier(Double surgeMultiplier) { this.surgeMultiplier = surgeMultiplier; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
