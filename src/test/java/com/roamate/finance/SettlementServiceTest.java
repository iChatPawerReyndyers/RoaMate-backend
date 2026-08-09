package com.roamate.finance;

import com.roamate.common.Money;
import com.roamate.finance.domain.PaymentSource;
import com.roamate.finance.dto.CreateExpenseRequest;
import com.roamate.finance.dto.NetBalance;
import com.roamate.finance.dto.PaymentLineDto;
import com.roamate.finance.dto.SettlementSummary;
import com.roamate.finance.repo.ExpenseRepository;
import com.roamate.finance.repo.KittyDepositRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves the FIN-05 invariant: for any expense, sum(fair shares) == total,
 * and across a whole trip, sum(all net deltas) == 0. This is the property
 * that must never break, since it's the difference between a trustworthy
 * group ledger and a broken one.
 */
class SettlementServiceTest {

    @Test
    void moneySplitAssignsEntireRemainderToDesignatedRecipientOnly() {
        // FIN-05 / edge case #3: $100.00 split 3 ways leaves a $0.01 delta.
        // Per spec, that whole delta goes to ONE recipient (the expense
        // logger) - it is never spread across multiple participants.
        Money total = Money.ofCents(10000);
        Money[] shares = total.splitEvenlyRemainderToRecipient(3, 1); // recipient at index 1

        long sum = 0;
        for (Money share : shares) sum += share.cents();

        assertEquals(10000, sum, "split shares must sum exactly to the original amount");
        assertEquals(3333, shares[0].cents());
        assertEquals(3334, shares[1].cents(), "the designated recipient absorbs the entire remainder");
        assertEquals(3333, shares[2].cents());
    }

    @Test
    void moneySplitHandlesMultiCentRemainderInOneRecipient() {
        // $100.01 split 3 ways: base=3333, remainder=2 cents, both go to one recipient.
        Money total = Money.ofCents(10001);
        Money[] shares = total.splitEvenlyRemainderToRecipient(3, 0);

        assertEquals(3335, shares[0].cents());
        assertEquals(3333, shares[1].cents());
        assertEquals(3333, shares[2].cents());
    }

    @Test
    void settlementNetDeltasSumToZeroAcrossTrip() {
        ExpenseRepository expenseRepository = Mockito.mock(ExpenseRepository.class);
        KittyDepositRepository kittyDepositRepository = Mockito.mock(KittyDepositRepository.class);
        DuplicateDetectionService duplicateDetectionService =
                new DuplicateDetectionService(expenseRepository);
        SettlementService settlementService =
                new SettlementService(expenseRepository, kittyDepositRepository, duplicateDetectionService, Mockito.mock(SimpMessagingTemplate.class));

        UUID tripId = UUID.randomUUID();

        // Alice abonos $50, split evenly among Alice, Bob, Carol ($16.67/$16.67/$16.66)
        CreateExpenseRequest request = new CreateExpenseRequest(
                tripId,
                "Dinner",
                "Food",
                Instant.now(),
                List.of(new PaymentLineDto(PaymentSource.MEMBER_ABONO, "alice", 5000)),
                List.of("alice", "bob", "carol"),
                "alice"
        );

        // Mock save() to just return the same entity (as JPA would after persist)
        Mockito.when(expenseRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(expenseRepository.findByTripIdAndDescriptionIgnoreCaseAndExpenseDateBetweenAndDeletedFalse(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(List.of());

        var expense = settlementService.createExpense(request);

        Mockito.when(expenseRepository.findByTripIdAndDeletedFalse(tripId)).thenReturn(List.of(expense));
        Mockito.when(kittyDepositRepository.findByTripIdAndDeletedFalse(tripId)).thenReturn(List.of());

        SettlementSummary summary = settlementService.computeSettlement(tripId);

        long sumOfDeltas = summary.balances().stream().mapToLong(NetBalance::netDeltaCents).sum();
        assertEquals(0, sumOfDeltas, "net deltas must always sum to zero (money cannot appear or vanish)");

        // $50.00 / 3 = base 1666 cents each, remainder 2 cents, all assigned
        // to alice as the expense logger (createdByUserId), per the FIN-05 rule.
        NetBalance aliceBalance = summary.balances().stream()
                .filter(b -> b.userId().equals("alice")).findFirst().orElseThrow();
        assertEquals(5000 - 1668, aliceBalance.netDeltaCents(), "alice paid $50, owes $16.68 (absorbs remainder) -> net +$33.32");
    }

    @Test
    void rejectsMultipleFillRemainingBalanceLines() {
        ExpenseRepository expenseRepository = Mockito.mock(ExpenseRepository.class);
        KittyDepositRepository kittyDepositRepository = Mockito.mock(KittyDepositRepository.class);
        DuplicateDetectionService duplicateDetectionService =
                new DuplicateDetectionService(expenseRepository);
        SettlementService settlementService =
                new SettlementService(expenseRepository, kittyDepositRepository, duplicateDetectionService, Mockito.mock(SimpMessagingTemplate.class));

        CreateExpenseRequest request = new CreateExpenseRequest(
                UUID.randomUUID(), "Hotel", "Lodging", Instant.now(),
                List.of(
                        new PaymentLineDto(PaymentSource.MEMBER_ABONO, "alice", -1),
                        new PaymentLineDto(PaymentSource.MEMBER_ABONO, "bob", -1)
                ),
                List.of("alice", "bob"),
                "alice"
        );

        assertThrows(IllegalArgumentException.class, () -> settlementService.createExpense(request));
    }
}
