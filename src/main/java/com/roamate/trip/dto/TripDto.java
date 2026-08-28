package com.roamate.trip.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TripDto(
        UUID id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String inviteCode,
        String inviteSecret,
        String defaultCurrency,
        List<TripMemberDto> members
) {}