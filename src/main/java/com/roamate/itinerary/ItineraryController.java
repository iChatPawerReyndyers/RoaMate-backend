package com.roamate.itinerary;

import com.roamate.trip.TripMemberRepository;
import com.roamate.trip.TripRole;
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
    public List<Destination> list(@PathVariable UUID tripId) {
        return itineraryService.listByTrip(tripId);
    }

    @PostMapping("/destinations")
    public Destination pin(@RequestBody Destination destination,
                            @AuthenticationPrincipal(expression = "subject") String userId) {
        requireAdmin(destination.getTripId(), userId);
        return itineraryService.pinDestination(destination);
    }

    @PostMapping("/destinations/reorder")
    public void reorder(@RequestBody List<UUID> orderedIds,
                        @AuthenticationPrincipal(expression = "subject") String userId) {
        if (!orderedIds.isEmpty()) {
            requireAdmin(itineraryService.tripIdForDestination(orderedIds.get(0)), userId);
        }
        itineraryService.reorder(orderedIds);
    }

    @PostMapping("/notes")
    public LocationNote addNote(@RequestBody LocationNote note) {
        // TRIP-02: adding location notes is explicitly listed as something
        // "all participants" can do - no admin check here, unlike pin/reorder
        // above.
        return itineraryService.addNote(note);
    }

    @GetMapping("/destinations/{destinationId}/notes")
    public List<LocationNote> listNotes(@PathVariable UUID destinationId) {
        return itineraryService.listNotes(destinationId);
    }

    /**
     * TRIP-02: "Admin can edit core itineraries; all participants can log
     * expenses, add location notes, track activities, manage checklists,
     * review financial conflicts, and configure privacy toggles." Itinerary
     * edits (pin/reorder) are the one action in the whole spec actually
     * restricted to admins - deliberately not applied anywhere else.
     */
    private void requireAdmin(UUID tripId, String userId) {
        boolean isAdmin = tripMemberRepository.findByTripIdAndUserId(tripId, userId)
                .map(member -> member.getRole() == TripRole.OWNER || member.getRole() == TripRole.CO_ORGANIZER)
                .orElse(false);
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trip admins can edit the itinerary");
        }
    }
}
