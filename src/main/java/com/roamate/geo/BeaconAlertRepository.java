package com.roamate.geo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BeaconAlertRepository extends JpaRepository<BeaconAlert, UUID> {
    List<BeaconAlert> findByTripIdAndAcknowledgedFalse(UUID tripId);
}
