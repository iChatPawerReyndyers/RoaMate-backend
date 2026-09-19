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
        /**
         * FIN-04 (custom split): optional, fully-resolved per-participant
         * shares - when present (and non-empty), these are used instead of
         * splitting evenly across participantUserIds. Null/empty preserves
         * the original even-split behavior exactly, so existing callers
         * that only ever sent participantUserIds are unaffected - see
         * SettlementService.createExpense for where this branches.
         */
        List<ParticipantShareDto> participantShares,
        String createdByUserId
) {}
