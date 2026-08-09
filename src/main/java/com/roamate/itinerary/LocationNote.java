package com.roamate.itinerary;

import com.roamate.common.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

/** ITIN-03: a collaborative note thread attached to a destination. */
@Entity
@Table(name = "location_notes")
public class LocationNote extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_id")
    private Destination destination;

    @Column(nullable = false)
    private String authorUserId;

    @Column(nullable = false, length = 2000)
    private String body;

    public Destination getDestination() { return destination; }
    public void setDestination(Destination destination) { this.destination = destination; }
    public String getAuthorUserId() { return authorUserId; }
    public void setAuthorUserId(String authorUserId) { this.authorUserId = authorUserId; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
