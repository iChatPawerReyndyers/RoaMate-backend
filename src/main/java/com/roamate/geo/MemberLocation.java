package com.roamate.geo;

import com.roamate.common.BaseEntity;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

/**
 * GEO-01..04: the LATEST on-demand location per member. There is
 * deliberately no history table of continuous pings - RoaMate never
 * background-tracks. Each row is overwritten when a member responds to a
 * silent push location request.
 */
@Entity
@Table(name = "member_locations")
public class MemberLocation extends BaseEntity {

    @Column(nullable = false)
    private UUID tripId;

    @Column(nullable = false)
    private String userId;

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point coordinates;

    @Column(nullable = false)
    private Instant capturedAt;

    /** True if this row is a stale, last-known-cache fallback per GEO-04 (device didn't respond within 5s). */
    private boolean stale = false;

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Point getCoordinates() { return coordinates; }
    public void setCoordinates(Point coordinates) { this.coordinates = coordinates; }
    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }
    public boolean isStale() { return stale; }
    public void setStale(boolean stale) { this.stale = stale; }
}
