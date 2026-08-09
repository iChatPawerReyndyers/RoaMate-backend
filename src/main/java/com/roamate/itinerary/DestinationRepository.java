package com.roamate.itinerary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DestinationRepository extends JpaRepository<Destination, UUID> {
    List<Destination> findByTripIdAndDeletedFalseOrderByAssignedDayAscSortOrderAsc(UUID tripId);
}
