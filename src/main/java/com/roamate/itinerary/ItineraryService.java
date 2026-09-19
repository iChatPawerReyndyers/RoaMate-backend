package com.roamate.itinerary;

import com.roamate.itinerary.dto.DestinationDto;
import com.roamate.itinerary.dto.PinDestinationRequest;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ItineraryService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    /** ITIN-06: upper bound for a stop's planned stay (24 hours) - mirrors the mobile field's max. */
    private static final int MAX_PLANNED_DURATION_MINUTES = 24 * 60;

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
        // ITIN-06: like every other field above this is a full overwrite - the
        // client always sends the stop's current value back (null = no duration).
        Integer plannedMinutes = request.plannedDurationMinutes();
        if (plannedMinutes != null && (plannedMinutes < 1 || plannedMinutes > MAX_PLANNED_DURATION_MINUTES)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "plannedDurationMinutes must be between 1 and " + MAX_PLANNED_DURATION_MINUTES);
        }
        destination.setPlannedDurationMinutes(plannedMinutes);

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
                d.getPriority(),
                d.getPlannedDurationMinutes(),
                d.getActivityCompletedAt()
        );
    }

    /**
     * ACT-05: marks the activity tracked at this stop as done, so the
     * mobile card can finally show its accumulated metrics (see
     * Destination.activityCompletedAt's doc comment for why this can't
     * just be inferred from ActivitySession existing). Also the
     * EventApplier target for the offline-queued equivalent - see
     * DestinationActivityCompletedApplier.
     */
    @Transactional
    public DestinationDto markActivityCompleted(UUID destinationId) {
        Destination destination = destinationRepository.findById(destinationId).orElseThrow();
        destination.setActivityCompletedAt(Instant.now());
        return toDto(destinationRepository.save(destination));
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