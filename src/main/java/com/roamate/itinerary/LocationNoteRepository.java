package com.roamate.itinerary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocationNoteRepository extends JpaRepository<LocationNote, UUID> {
    List<LocationNote> findByDestinationIdOrderByCreatedAtAsc(UUID destinationId);
}
