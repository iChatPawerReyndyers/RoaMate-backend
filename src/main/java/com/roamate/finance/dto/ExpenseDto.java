package com.roamate.finance.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * FIN-05: raw per-expense shape, distinct from SettlementSummary's
 * aggregated-only view. Powers the mobile client's offline settlement
 * cache (expensesRepository.ts + SettlementEngine.ts) - the client mirrors
 * this list locally so FinanceSummaryScreen can still show balances when
 * the live /settlement endpoint isn't reachable.
 */
public record ExpenseDto(
        UUID id,
        String description,
        long totalAmountCents,
        Instant expenseDate,
        String category,
        String createdByUserId,
        boolean flaggedDuplicate,
        List<PaymentLineOut> payments,
        List<ParticipantOut> participants
) {
    public record PaymentLineOut(String source, String payerUserId, long amountPaidCents) {}

    public record ParticipantOut(String userId, long fairShareCents) {}
}