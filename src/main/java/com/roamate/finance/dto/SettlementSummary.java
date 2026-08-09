package com.roamate.finance.dto;

import java.util.List;

public record SettlementSummary(java.util.UUID tripId, List<NetBalance> balances, List<SuggestedTransfer> suggestedTransfers) {}
