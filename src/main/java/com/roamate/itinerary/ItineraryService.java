package com.roamate.itinerary;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ItineraryService {

    private final DestinationRepository destinationRepository;
    private final LocationNoteRepository locationNoteRepository;

    public ItineraryService(DestinationRepository destinationRepository, LocationNoteRepository locationNoteRepository) {
        this.destinationRepository = destinationRepository;
        this.locationNoteRepository = locationNoteRepository;
    }

    public List<Destination> listByTrip(UUID tripId) {
        return destinationRepository.findByTripIdAndDeletedFalseOrderByAssignedDayAscSortOrderAsc(tripId);
    }

    public UUID tripIdForDestination(UUID destinationId) {
        return destinationRepository.findById(destinationId).orElseThrow().getTripId();
    }

    @Transactional
    public Destination pinDestination(Destination destination) {
        return destinationRepository.save(destination);
    }

    /** ITIN-01: persists a new drag-reorder sequence for a day's stops. */
    @Transactional
    public void reorder(List<UUID> destinationIdsInOrder) {
        for (int i = 0; i < destinationIdsInOrder.size(); i++) {
            Destination d = destinationRepository.findById(destinationIdsInOrder.get(i)).orElseThrow();
            d.setSortOrder(i);
            destinationRepository.save(d);
        }
    }

    @Transactional
    public LocationNote addNote(LocationNote note) {
        return locationNoteRepository.save(note);
    }

    public List<LocationNote> listNotes(UUID destinationId) {
        return locationNoteRepository.findByDestinationIdOrderByCreatedAtAsc(destinationId);
    }
}
