package com.roamate.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateExpenseRequest(
        @NotNull UUID tripId,
        @NotBlank String description,
        String category,
        @NotNull Instant expenseDate,
        @NotEmpty List<PaymentLineDto> payments,
        @NotEmpty List<String> participantUserIds,
        String createdByUserId
) {}
