package com.roamate.itinerary.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * ITIN-02/04: flat lat/lng output shape for a pinned destination - mirrors
 * the pattern LocationController and RaiseBeaconRequest already use for
 * member locations and beacons, since Destination.coordinates is a JTS
 * Point with no Jackson-visible getters that produce a clean {lat, lng}
 * shape. The mobile client's Destination interface (ItineraryScreen.tsx)
 * already expects lat/lng at the top level, so returning the raw entity
 * here previously left those fields undefined on every destination.
 */
public record DestinationDto(
        UUID id,
        UUID tripId,
        String name,
        Double lat,
        Double lng,
        LocalDate assignedDay,
        int sortOrder,
        String notes,
        String address,
        String operatingHours,
        Long targetBudgetCents,
        String attachmentUrls,
        String priority
) {}
