package com.roamate.trip.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TripDto(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        String inviteCode,
        String defaultCurrency,
        List<TripMemberDto> members
) {}
