package com.roamate.itinerary.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * ITIN-02/04: flat request shape for creating or editing a pinned
 * destination. Destination.coordinates is a JTS Point with no
 * Jackson-visible constructor - same issue RaiseBeaconRequest already
 * works around by taking flat lat/lng and building the Point server-side
 * (see BeaconService) rather than expecting the client to send a Point
 * directly. Previously the controller took the raw Destination entity as
 * the request body, so a client-supplied lat/lng (e.g. from tapping a pin
 * on the map) had nowhere to land - this is what MapScreen's "save to
 * itinerary" flow now posts.
 */
public record PinDestinationRequest(
        UUID id,
        UUID tripId,
        String name,
        Double lat,
        Double lng,
        LocalDate assignedDay,
        String notes,
        String address,
        String operatingHours,
        Long targetBudgetCents,
        String attachmentUrls,
        String priority
) {}
