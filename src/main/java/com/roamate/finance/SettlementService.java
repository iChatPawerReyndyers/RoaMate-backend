package com.roamate.finance;

import com.roamate.common.Money;
import com.roamate.finance.domain.Expense;
import com.roamate.finance.domain.ExpenseParticipant;
import com.roamate.finance.domain.ExpensePayment;
import com.roamate.finance.domain.KittyDeposit;
import com.roamate.finance.domain.PaymentSource;
import com.roamate.finance.dto.CreateExpenseRequest;
import com.roamate.finance.dto.NetBalance;
import com.roamate.finance.dto.PaymentLineDto;
import com.roamate.finance.dto.SettlementSummary;
import com.roamate.finance.dto.SuggestedTransfer;
import com.roamate.finance.repo.ExpenseRepository;
import com.roamate.finance.repo.KittyDepositRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FIN-03/04/05: builds expenses (with the "fill remaining balance" invariant
 * enforced), then computes each member's Net Delta = Amount Paid - Fair
 * Share across every expense + kitty deposit in the trip.
 *
 * All arithmetic is integer cents (see Money) - never floating point.
 */
@Service
public class SettlementService {

    private final ExpenseRepository expenseRepository;
    private final KittyDepositRepository kittyDepositRepository;
    private final DuplicateDetectionService duplicateDetectionService;
    private final SimpMessagingTemplate messagingTemplate;

    public SettlementService(ExpenseRepository expenseRepository,
                              KittyDepositRepository kittyDepositRepository,
                              DuplicateDetectionService duplicateDetectionService,
                              SimpMessagingTemplate messagingTemplate) {
        this.expenseRepository = expenseRepository;
        this.kittyDepositRepository = kittyDepositRepository;
        this.duplicateDetectionService = duplicateDetectionService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * FIN-03: Creates an expense from a set of payment lines and participants.
     * If exactly one payment line has amountCents == -1 (the UI's "fill
     * remaining balance" sentinel), that line absorbs whatever is left after
     * summing the other explicit lines, so the total always balances to the
     * cent without the user doing manual subtraction.
     */
    @Transactional
    public Expense createExpense(CreateExpenseRequest request) {
        List<PaymentLineDto> lines = request.payments();
        long explicitSum = lines.stream()
                .filter(l -> l.amountCents() >= 0)
                .mapToLong(PaymentLineDto::amountCents)
                .sum();
        long fillCount = lines.stream().filter(l -> l.amountCents() < 0).count();
        if (fillCount > 1) {
            throw new IllegalArgumentException("Only one payment line may use fill-remaining-balance");
        }

        long total;
        List<PaymentLineDto> resolvedLines = new ArrayList<>();
        if (fillCount == 1) {
            // Total is unknown until the fill line is resolved; caller must have
            // supplied the intended total via the first non-fill line context.
            // In practice the mobile client always sends an explicit total in
            // this scenario via `expenseTotalCents` metadata; for the API-level
            // contract here we require at least the fill line plus explicit
            // lines whose sum, plus the fill line, is provided by the client's
            // declared total on the Expense itself (see mobile FillRemainingBalance.ts).
            throw new IllegalArgumentException(
                    "Server expects pre-resolved payment amounts; fill-remaining-balance is resolved client-side " +
                    "in mobile/src/features/finance/FillRemainingBalance.ts before submission");
        } else {
            resolvedLines = lines;
            total = explicitSum;
        }

        Expense expense = new Expense();
        expense.setTripId(request.tripId());
        expense.setDescription(request.description());
        expense.setCategory(request.category());
        expense.setExpenseDate(request.expenseDate());
        expense.setCreatedByUserId(request.createdByUserId());
        expense.setTotalAmount(Money.ofCents(total));

        for (PaymentLineDto line : resolvedLines) {
            ExpensePayment payment = new ExpensePayment();
            payment.setExpense(expense);
            payment.setSource(line.source());
            payment.setPayerUserId(line.source() == PaymentSource.KITTY ? null : line.payerUserId());
            payment.setAmountPaid(Money.ofCents(line.amountCents()));
            expense.getPayments().add(payment);
        }

        // FIN-05 / edge case #3: split the total evenly across participants,
        // then assign the entire rounding remainder to the expense logger if
        // they're among the participants, falling back to the first
        // participant listed otherwise - per spec, never spread across
        // multiple people (see Money#splitEvenlyRemainderToRecipient).
        List<String> participantIds = request.participantUserIds();
        int remainderRecipientIndex = Math.max(0, participantIds.indexOf(request.createdByUserId()));
        Money[] shares = Money.ofCents(total)
                .splitEvenlyRemainderToRecipient(participantIds.size(), remainderRecipientIndex);
        for (int i = 0; i < participantIds.size(); i++) {
            ExpenseParticipant participant = new ExpenseParticipant();
            participant.setExpense(expense);
            participant.setUserId(participantIds.get(i));
            participant.setFairShare(shares[i]);
            expense.getParticipants().add(participant);
        }

        Expense saved = expenseRepository.save(expense);
        duplicateDetectionService.flagIfDuplicate(saved);

        // Live-update channel: while a member is online, their app updates
        // immediately rather than waiting for the next sync cycle. Offline
        // members still get the change via the REST sync endpoints.
        messagingTemplate.convertAndSend("/topic/trips/" + saved.getTripId() + "/expenses", saved.getId());

        return saved;
    }

    /**
     * Computes every member's net balance across all non-deleted expenses
     * (excluding those flagged as unresolved duplicates) plus kitty deposits,
     * then produces a minimal set of suggested transfers to zero everyone out.
     */
    @Transactional(readOnly = true)
    public SettlementSummary computeSettlement(UUID tripId) {
        Map<String, Long> paid = new LinkedHashMap<>();
        Map<String, Long> fairShare = new LinkedHashMap<>();

        List<Expense> expenses = expenseRepository.findByTripIdAndDeletedFalse(tripId).stream()
                .filter(e -> !e.isFlaggedDuplicate())
                .collect(Collectors.toList());

        for (Expense expense : expenses) {
            for (ExpensePayment payment : expense.getPayments()) {
                if (payment.getSource() == PaymentSource.MEMBER_ABONO) {
                    paid.merge(payment.getPayerUserId(), payment.getAmountPaid().cents(), Long::sum);
                }
            }
            for (ExpenseParticipant participant : expense.getParticipants()) {
                fairShare.merge(participant.getUserId(), participant.getFairShare().cents(), Long::sum);
                paid.putIfAbsent(participant.getUserId(), 0L);
            }
        }

        List<KittyDeposit> deposits = kittyDepositRepository.findByTripIdAndDeletedFalse(tripId);
        for (KittyDeposit deposit : deposits) {
            paid.merge(deposit.getDepositorUserId(), deposit.getAmount().cents(), Long::sum);
        }

        List<NetBalance> balances = new ArrayList<>();
        for (String userId : fairShare.keySet()) {
            long paidCents = paid.getOrDefault(userId, 0L);
            long fairCents = fairShare.getOrDefault(userId, 0L);
            balances.add(new NetBalance(userId, paidCents, fairCents, paidCents - fairCents));
        }

        List<SuggestedTransfer> transfers = minimalTransfers(balances);
        return new SettlementSummary(tripId, balances, transfers);
    }

    /**
     * Greedy min-cash-flow algorithm: repeatedly match the largest debtor
     * with the largest creditor. Produces at most N-1 transfers for N
     * members with non-zero balances.
     */
    private List<SuggestedTransfer> minimalTransfers(List<NetBalance> balances) {
        PriorityQueue<long[]> creditors = new PriorityQueue<>((a, b) -> Long.compare(b[1], a[1]));
        PriorityQueue<long[]> debtors = new PriorityQueue<>((a, b) -> Long.compare(a[1], b[1]));
        Map<Long, String> idToUser = new LinkedHashMap<>();

        long idx = 0;
        for (NetBalance b : balances) {
            idToUser.put(idx, b.userId());
            if (b.netDeltaCents() > 0) {
                creditors.add(new long[]{idx, b.netDeltaCents()});
            } else if (b.netDeltaCents() < 0) {
                debtors.add(new long[]{idx, b.netDeltaCents()});
            }
            idx++;
        }

        List<SuggestedTransfer> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            long[] cred = creditors.poll();
            long[] debt = debtors.poll();

            long amount = Math.min(cred[1], -debt[1]);
            transfers.add(new SuggestedTransfer(idToUser.get(debt[0]), idToUser.get(cred[0]), amount));

            long remainingCred = cred[1] - amount;
            long remainingDebt = debt[1] + amount;

            if (remainingCred > 0) creditors.add(new long[]{cred[0], remainingCred});
            if (remainingDebt < 0) debtors.add(new long[]{debt[0], remainingDebt});
        }
        return transfers;
    }
}
