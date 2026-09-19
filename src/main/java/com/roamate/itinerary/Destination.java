package com.roamate.itinerary;

import com.roamate.common.BaseEntity;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** ITIN-02: a pinned destination with an optional day assignment and attachments. */
@Entity
@Table(name = "destinations")
public class Destination extends BaseEntity {

    @Column(nullable = false)
    private UUID tripId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "geometry(Point,4326)")
    private Point coordinates;

    private LocalDate assignedDay;

    /** ITIN-01: drag-reorder position within its assigned day. */
    private int sortOrder;

    private String notes;

    /** ITIN-02: street address, distinct from the geotagged coordinates. */
    private String address;

    /** ITIN-02: e.g. "8:00 AM - 5:00 PM" or "Registration closes 8:00 AM". */
    private String operatingHours;

    /** ITIN-02: target budget allocation for this stop, in integer cents (never float). */
    private Long targetBudgetCents;

    /** Comma-separated attachment URLs (photos, PDFs) uploaded via the mobile client. */
    private String attachmentUrls;

    /** ITIN-06: optional planned stay length in whole minutes (e.g. 150 = 2 hrs 30 mins); null = not set. */
    private Integer plannedDurationMinutes;

    /** ITIN-03: REQUIRED, OPTIONAL, or TENTATIVE - defaults to REQUIRED for existing rows. */
    @Column(nullable = false)
    private String priority = "REQUIRED";

    /**
     * ACT-05: when the activity tracked at this stop was explicitly marked
     * done (see ItineraryService.markActivityCompleted). Null means "not
     * finished yet" - the Pinned Location Card gates on this rather than
     * on ActivitySession existence, since sessions get created mid-activity
     * (periodic pedometer batches, an elevation stop-and-upload) well
     * before the traveler is actually done with this stop. Left nullable
     * and not reset automatically on a later "start activity here" tap, so
     * a finished stop stays finished (and keeps showing its metrics) even
     * if someone logs an additional session there afterward.
     */
    private Instant activityCompletedAt;

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Point getCoordinates() { return coordinates; }
    public void setCoordinates(Point coordinates) { this.coordinates = coordinates; }
    public LocalDate getAssignedDay() { return assignedDay; }
    public void setAssignedDay(LocalDate assignedDay) { this.assignedDay = assignedDay; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }
    public Long getTargetBudgetCents() { return targetBudgetCents; }
    public void setTargetBudgetCents(Long targetBudgetCents) { this.targetBudgetCents = targetBudgetCents; }
    public String getAttachmentUrls() { return attachmentUrls; }
    public void setAttachmentUrls(String attachmentUrls) { this.attachmentUrls = attachmentUrls; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Integer getPlannedDurationMinutes() { return plannedDurationMinutes; }
    public void setPlannedDurationMinutes(Integer plannedDurationMinutes) { this.plannedDurationMinutes = plannedDurationMinutes; }
    public Instant getActivityCompletedAt() { return activityCompletedAt; }
    public void setActivityCompletedAt(Instant activityCompletedAt) { this.activityCompletedAt = activityCompletedAt; }
}