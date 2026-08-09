package com.roamate.sync;

import com.roamate.common.BaseEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only event log. Mobile clients queue events locally while offline
 * (see mobile/src/sync/EventQueue.ts) and POST them in original
 * chronological (client-timestamp) order on reconnect. The server never
 * mutates a past event - conflicts are resolved by replaying events in
 * order and surfacing collisions (e.g. duplicate expenses) for human
 * review rather than silently picking a winner.
 */
@Entity
@Table(name = "event_log")
public class EventLogEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID tripId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(nullable = false)
    private String originDeviceId;

    @Column(nullable = false)
    private String originUserId;

    /** Timestamp the event actually occurred on-device, NOT when it reached the server. */
    @Column(nullable = false)
    private Instant clientTimestamp;

    // NOT @Lob: Hibernate 7 maps @Lob on String to a PostgreSQL large object
    // (oid), not a plain TEXT column. The migration creates this as TEXT,
    // which is what we actually want for JSON payloads - explicit
    // columnDefinition keeps Hibernate's schema validation aligned with it.
    @Column(nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Column(nullable = false)
    private boolean applied = false;

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public String getOriginDeviceId() { return originDeviceId; }
    public void setOriginDeviceId(String originDeviceId) { this.originDeviceId = originDeviceId; }
    public String getOriginUserId() { return originUserId; }
    public void setOriginUserId(String originUserId) { this.originUserId = originUserId; }
    public Instant getClientTimestamp() { return clientTimestamp; }
    public void setClientTimestamp(Instant clientTimestamp) { this.clientTimestamp = clientTimestamp; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }
}