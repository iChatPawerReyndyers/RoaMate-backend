package com.roamate.itinerary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LocationNoteRepository extends JpaRepository<LocationNote, UUID> {
    List<LocationNote> findByDestinationIdOrderByCreatedAtAsc(UUID destinationId);

    // LocationNote has no tripId column of its own - it only reaches a trip
    // via destination.tripId - so this is a join query rather than a
    // derived-name finder like the others.
    @Query("select n from LocationNote n where n.destination.tripId = :tripId order by n.createdAt asc")
    List<LocationNote> findByTripId(@Param("tripId") UUID tripId);
}
