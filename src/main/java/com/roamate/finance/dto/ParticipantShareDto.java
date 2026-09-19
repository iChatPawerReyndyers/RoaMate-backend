package com.roamate.finance.dto;

/**
 * FIN-04 (custom split): one participant's already-resolved cost share.
 * "Already-resolved" mirrors PaymentLineDto's contract on the payments
 * side - the mobile client always sends concrete cents here (see
 * mobile FillRemainingBalance.ts's resolveEvenSplitRemaining), never a
 * fill-remaining sentinel, so SettlementService only needs to validate
 * the sum, not resolve anything itself.
 */
public record ParticipantShareDto(String userId, long amountCents) {}
