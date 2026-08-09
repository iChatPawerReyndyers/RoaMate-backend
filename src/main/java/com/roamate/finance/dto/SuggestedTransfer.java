package com.roamate.finance.dto;

/** A minimal-transaction-count "who pays whom" suggestion to zero out all balances. */
public record SuggestedTransfer(String fromUserId, String toUserId, long amountCents) {}
