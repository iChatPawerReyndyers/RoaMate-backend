package com.roamate.itinerary;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/itinerary")
public class ItineraryController {

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @GetMapping("/trips/{tripId}/destinations")
    public List<Destination> list(@PathVariable UUID tripId) {
        return itineraryService.listByTrip(tripId);
    }

    @PostMapping("/destinations")
    public Destination pin(@RequestBody Destination destination) {
        return itineraryService.pinDestination(destination);
    }

    @PostMapping("/destinations/reorder")
    public void reorder(@RequestBody List<UUID> orderedIds) {
        itineraryService.reorder(orderedIds);
    }

    @PostMapping("/notes")
    public LocationNote addNote(@RequestBody LocationNote note) {
        return itineraryService.addNote(note);
    }

    @GetMapping("/destinations/{destinationId}/notes")
    public List<LocationNote> listNotes(@PathVariable UUID destinationId) {
        return itineraryService.listNotes(destinationId);
    }
}
