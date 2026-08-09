package com.roamate.geo;

import com.roamate.common.BaseEntity;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

/** GEO-05: emergency beacon - broadcasts the sender's current location to every trip member. */
@Entity
@Table(name = "beacon_alerts")
public class BeaconAlert extends BaseEntity {

    @Column(nullable = false)
    private UUID tripId;

    @Column(nullable = false)
    private String raisedByUserId;

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point coordinates;

    @Column(nullable = false)
    private Instant raisedAt;

    private boolean acknowledged = false;

    /** GEO-05: one-tap status - "Arrived Safely", "Need Assistance", etc. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BeaconStatus status = BeaconStatus.NEED_ASSISTANCE;

    /** Optional free-text detail, e.g. "at Pin X" / "twisted ankle near the summit trail". */
    private String message;

    /** Optional link to the itinerary pin this status references (e.g. "Need Assistance at Pin X"). */
    private UUID destinationId;

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }
    public String getRaisedByUserId() { return raisedByUserId; }
    public void setRaisedByUserId(String raisedByUserId) { this.raisedByUserId = raisedByUserId; }
    public Point getCoordinates() { return coordinates; }
    public void setCoordinates(Point coordinates) { this.coordinates = coordinates; }
    public Instant getRaisedAt() { return raisedAt; }
    public void setRaisedAt(Instant raisedAt) { this.raisedAt = raisedAt; }
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    public BeaconStatus getStatus() { return status; }
    public void setStatus(BeaconStatus status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public UUID getDestinationId() { return destinationId; }
    public void setDestinationId(UUID destinationId) { this.destinationId = destinationId; }
}
