package com.roamate.trip.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinTripRequest(
        @NotBlank String inviteCode,
        @NotBlank String displayName,
        /**
         * TRIP-01: present only when joining via ScanQRScreen (the QR
         * payload carries it); absent/null for a manually typed invite
         * code. Validated in TripService#joinTrip when present.
         */
        String inviteSecret
) {}