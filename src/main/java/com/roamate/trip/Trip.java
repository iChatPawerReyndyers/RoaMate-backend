package com.roamate.trip;

import com.roamate.common.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** TRIP-01: a trip is the top-level container all other data hangs off. */
@Entity
@Table(name = "trips")
public class Trip extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private LocalDate startDate;
    private LocalDate endDate;

    /** 6-character alphanumeric invite code (TRIP-01). Unique, uppercase. */
    @Column(nullable = false, unique = true, length = 6)
    private String inviteCode;

    @Column(nullable = false)
    private String defaultCurrency = "USD";

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripMember> members = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
    public List<TripMember> getMembers() { return members; }
}
