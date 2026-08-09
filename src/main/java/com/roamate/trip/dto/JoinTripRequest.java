package com.roamate.trip.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinTripRequest(
        @NotBlank String inviteCode,
        @NotBlank String displayName
) {}
