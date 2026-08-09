package com.roamate.activity;

import com.roamate.common.BaseEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * ACT-01..04: a batch of sensor readings uploaded from the mobile device.
 * Batching (every 10-15s or 50 steps, per the mobile PedometerService) keeps
 * this table from being flooded with per-step writes.
 */
@Entity
@Table(name = "activity_sessions")
public class ActivitySession extends BaseEntity {

    @Column(nullable = false)
    private UUID tripId;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    private Integer stepCount;
    private Double distanceMeters;

    /** Section 8.1's `duration_s`: active tracking time for this session. */
    private Long durationSeconds;

    /** For MOUNTAIN_ELEVATION: absolute GPS+barometer altitude gain in meters. */
    private Double elevationGainMeters;

    /** Section 8.1's `max_alt_m` / `min_alt_m`: highest and lowest absolute altitude reached (MOUNTAIN_ELEVATION only). */
    private Double maxAltitudeMeters;
    private Double minAltitudeMeters;

    /**
     * For CAVE_DEPTH: relative pressure-derived depth in meters below the
     * entrance baseline (0m = entrance, set when the session starts).
     * Corresponds to section 8.1's `cave_depth_m`.
     */
    private Double relativeDepthMeters;

    /** Optional link back to an itinerary stop this session was recorded against (ACT-04). */
    private UUID destinationId;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant lastBatchAt;

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public ActivityType getType() { return type; }
    public void setType(ActivityType type) { this.type = type; }
    public Integer getStepCount() { return stepCount; }
    public void setStepCount(Integer stepCount) { this.stepCount = stepCount; }
    public Double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    public Double getElevationGainMeters() { return elevationGainMeters; }
    public void setElevationGainMeters(Double elevationGainMeters) { this.elevationGainMeters = elevationGainMeters; }
    public Double getMaxAltitudeMeters() { return maxAltitudeMeters; }
    public void setMaxAltitudeMeters(Double maxAltitudeMeters) { this.maxAltitudeMeters = maxAltitudeMeters; }
    public Double getMinAltitudeMeters() { return minAltitudeMeters; }
    public void setMinAltitudeMeters(Double minAltitudeMeters) { this.minAltitudeMeters = minAltitudeMeters; }
    public Double getRelativeDepthMeters() { return relativeDepthMeters; }
    public void setRelativeDepthMeters(Double relativeDepthMeters) { this.relativeDepthMeters = relativeDepthMeters; }
    public UUID getDestinationId() { return destinationId; }
    public void setDestinationId(UUID destinationId) { this.destinationId = destinationId; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getLastBatchAt() { return lastBatchAt; }
    public void setLastBatchAt(Instant lastBatchAt) { this.lastBatchAt = lastBatchAt; }
}
