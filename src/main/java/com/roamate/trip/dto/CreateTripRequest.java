package com.roamate.trip.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateTripRequest(
        @NotBlank String name,
        LocalDate startDate,
        LocalDate endDate,
        String defaultCurrency
) {}
