package com.roamate.finance.dto;

import java.util.List;
import java.util.UUID;

/** FIN-08: a cluster of expenses flagged as potential duplicates of each other. */
public record DuplicateGroupDto(List<FlaggedExpenseDto> expenses) {

    public record FlaggedExpenseDto(
            UUID id,
            String description,
            long totalAmountCents,
            String expenseDateIso,
            String createdByUserId,
            boolean deleted
    ) {}
}
