package com.roamate.trip;

import com.roamate.common.BaseEntity;
import jakarta.persistence.*;

/** Join entity between a user and a trip, carrying the member's role. */
@Entity
@Table(name = "trip_members", uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "user_id"}))
public class TripMember extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripRole role = TripRole.MEMBER;

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public TripRole getRole() { return role; }
    public void setRole(TripRole role) { this.role = role; }
}
