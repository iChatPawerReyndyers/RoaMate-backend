package com.roamate.common;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Every syncable entity gets a client-generated UUID (so offline devices can
 * create IDs without a round trip), plus server-side audit timestamps and a
 * soft-delete flag used by the Conflict Review Dashboard (FIN-08).
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted = false;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
