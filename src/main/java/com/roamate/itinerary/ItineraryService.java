package com.roamate.itinerary;

import com.roamate.itinerary.dto.DestinationDto;
import com.roamate.itinerary.dto.PinDestinationRequest;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ItineraryService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final DestinationRepository destinationRepository;
    private final LocationNoteRepository locationNoteRepository;

    public ItineraryService(DestinationRepository destinationRepository, LocationNoteRepository locationNoteRepository) {
        this.destinationRepository = destinationRepository;
        this.locationNoteRepository = locationNoteRepository;
    }

    public List<DestinationDto> listByTrip(UUID tripId) {
        return destinationRepository.findByTripIdAndDeletedFalseOrderByAssignedDayAscSortOrderAsc(tripId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public UUID tripIdForDestination(UUID destinationId) {
        return destinationRepository.findById(destinationId).orElseThrow().getTripId();
    }

    /**
     * ITIN-02/04: builds or updates a Destination from the flat lat/lng
     * request shape (see PinDestinationRequest's doc comment for why this
     * can't just accept a JTS Point over JSON). Used both by the admin
     * "add/edit destination" form (DestinationFormScreen, lat/lng omitted)
     * and MapScreen's "save pin to itinerary" flow (lat/lng required).
     */
    @Transactional
    public DestinationDto pinDestination(PinDestinationRequest request) {
        Destination destination = request.id() != null
                ? destinationRepository.findById(request.id()).orElseGet(Destination::new)
                : new Destination();

        destination.setTripId(request.tripId());
        destination.setName(request.name());
        if (request.lat() != null && request.lng() != null) {
            destination.setCoordinates(GEOMETRY_FACTORY.createPoint(new Coordinate(request.lng(), request.lat())));
        }
        destination.setAssignedDay(request.assignedDay());
        destination.setNotes(request.notes());
        destination.setAddress(request.address());
        destination.setOperatingHours(request.operatingHours());
        destination.setTargetBudgetCents(request.targetBudgetCents());
        destination.setAttachmentUrls(request.attachmentUrls());
        destination.setPriority(request.priority() != null ? request.priority() : "REQUIRED");

        return toDto(destinationRepository.save(destination));
    }

    /**
     * Soft-delete a pinned destination (BaseEntity.deleted, same mechanism
     * the Conflict Review Dashboard uses for expenses) - listByTrip already
     * filters on deleted=false, so this just needs to flip the flag.
     */
    @Transactional
    public void deleteDestination(UUID destinationId) {
        Destination destination = destinationRepository.findById(destinationId).orElseThrow();
        destination.setDeleted(true);
        destinationRepository.save(destination);
    }

    private DestinationDto toDto(Destination d) {
        Point coordinates = d.getCoordinates();
        return new DestinationDto(
                d.getId(),
                d.getTripId(),
                d.getName(),
                coordinates != null ? coordinates.getY() : null,
                coordinates != null ? coordinates.getX() : null,
                d.getAssignedDay(),
                d.getSortOrder(),
                d.getNotes(),
                d.getAddress(),
                d.getOperatingHours(),
                d.getTargetBudgetCents(),
                d.getAttachmentUrls(),
                d.getPriority()
        );
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
