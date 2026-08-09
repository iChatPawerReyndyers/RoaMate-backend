package com.roamate.trip.dto;

import java.util.UUID;

public record TripMemberDto(
        UUID id,
        UUID tripId,
        String userId,
        String displayName,
        String role
) {}
