package com.roamate.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivitySessionRepository extends JpaRepository<ActivitySession, UUID> {
    List<ActivitySession> findByTripIdAndUserId(UUID tripId, String userId);
    List<ActivitySession> findByDestinationId(UUID destinationId);
    List<ActivitySession> findByTripId(UUID tripId);
}
