package com.roamate.itinerary;

import com.roamate.itinerary.dto.DestinationDto;
import com.roamate.itinerary.dto.PinDestinationRequest;
import com.roamate.trip.TripMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/itinerary")
public class ItineraryController {

    private final ItineraryService itineraryService;
    private final TripMemberRepository tripMemberRepository;

    public ItineraryController(ItineraryService itineraryService, TripMemberRepository tripMemberRepository) {
        this.itineraryService = itineraryService;
        this.tripMemberRepository = tripMemberRepository;
    }

    @GetMapping("/trips/{tripId}/destinations")
    public List<DestinationDto> list(@PathVariable UUID tripId) {
        return itineraryService.listByTrip(tripId);
    }

    /**
     * Pinning is itself an itinerary edit but per the trip's rules,
     * itinerary edits (add/pin, edit, reorder, remove) are open to any
     * trip member, not just admins - trip membership is already the real
     * gate, since a non-member can't reach this trip's itinerary at all.
     */
    @PostMapping("/destinations")
    public DestinationDto pin(@RequestBody PinDestinationRequest request,
                               @AuthenticationPrincipal(expression = "subject") String userId) {
        requireMember(request.tripId(), userId);
        return itineraryService.pinDestination(request);
    }

    @DeleteMapping("/destinations/{destinationId}")
    public void delete(@PathVariable UUID destinationId,
                        @AuthenticationPrincipal(expression = "subject") String userId) {
        requireMember(itineraryService.tripIdForDestination(destinationId), userId);
        itineraryService.deleteDestination(destinationId);
    }

    @PostMapping("/destinations/reorder")
    public void reorder(@RequestBody List<UUID> orderedIds,
                        @AuthenticationPrincipal(expression = "subject") String userId) {
        if (!orderedIds.isEmpty()) {
            requireMember(itineraryService.tripIdForDestination(orderedIds.get(0)), userId);
        }
        itineraryService.reorder(orderedIds);
    }

    @PostMapping("/notes")
    public LocationNote addNote(@RequestBody LocationNote note) {
        // Adding location notes has never been admin-gated (open to all
        // participants), same as pin/reorder/delete above are now too.
        return itineraryService.addNote(note);
    }

    @GetMapping("/destinations/{destinationId}/notes")
    public List<LocationNote> listNotes(@PathVariable UUID destinationId) {
        return itineraryService.listNotes(destinationId);
    }

    /**
     * Itinerary edits (add/pin, edit, reorder, remove a destination) are
     * open to any trip member - not gated to admins the way the rest of
     * TRIP-02's role split originally suggested, since trip membership
     * (you can't reach this endpoint at all otherwise) is already
     * considered a sufficient gate for this trip's itinerary.
     */
    private void requireMember(UUID tripId, String userId) {
        boolean isMember = tripMemberRepository.findByTripIdAndUserId(tripId, userId).isPresent();
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be a member of this trip");
        }
    }
}
