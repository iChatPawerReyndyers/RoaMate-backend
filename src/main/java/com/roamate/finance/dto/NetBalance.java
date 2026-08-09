package com.roamate.finance.dto;

/**
 * FIN-05: Net Delta = Amount Paid - Fair Share.
 * Positive netDeltaCents => this member is owed money.
 * Negative netDeltaCents => this member owes money.
 */
public record NetBalance(String userId, long totalPaidCents, long totalFairShareCents, long netDeltaCents) {}
