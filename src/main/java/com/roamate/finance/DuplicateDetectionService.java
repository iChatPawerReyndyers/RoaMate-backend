package com.roamate.finance;

import com.roamate.finance.domain.Expense;
import com.roamate.finance.dto.DuplicateGroupDto;
import com.roamate.finance.repo.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FIN-07: after an offline sync merges expenses from multiple devices, flag
 * (but never auto-delete) expenses that look like duplicates - same trip,
 * same description (case-insensitive), same amount, created within a
 * 5-10 minute window of another expense. Resolution is always a human
 * decision made in the Conflict Review Dashboard (FIN-08).
 */
@Service
public class DuplicateDetectionService {

    private static final int WINDOW_MINUTES = 10;

    private final ExpenseRepository expenseRepository;

    public DuplicateDetectionService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public void flagIfDuplicate(Expense candidate) {
        Instant from = candidate.getExpenseDate().minus(WINDOW_MINUTES, ChronoUnit.MINUTES);
        Instant to = candidate.getExpenseDate().plus(WINDOW_MINUTES, ChronoUnit.MINUTES);

        List<Expense> nearby = expenseRepository
                .findByTripIdAndDescriptionIgnoreCaseAndExpenseDateBetweenAndDeletedFalse(
                        candidate.getTripId(), candidate.getDescription(), from, to);

        boolean hasMatch = nearby.stream()
                .anyMatch(other -> !other.getId().equals(candidate.getId())
                        && other.getTotalAmount().equals(candidate.getTotalAmount()));

        if (hasMatch) {
            candidate.setFlaggedDuplicate(true);
            nearby.forEach(other -> {
                if (!other.getId().equals(candidate.getId())
                        && other.getTotalAmount().equals(candidate.getTotalAmount())) {
                    other.setFlaggedDuplicate(true);
                }
            });
        }
    }

    /**
     * FIN-08: everything currently flagged for a trip, clustered into groups
     * of mutually-matching expenses so the dashboard can show "these 2 look
     * like the same charge" rather than a flat list.
     */
    public List<DuplicateGroupDto> listDuplicateGroups(UUID tripId) {
        List<Expense> flagged = expenseRepository.findByTripIdAndFlaggedDuplicateTrue(tripId);
        List<List<Expense>> groups = new ArrayList<>();
        List<Expense> remaining = new ArrayList<>(flagged);

        while (!remaining.isEmpty()) {
            Expense seed = remaining.remove(0);
            List<Expense> group = new ArrayList<>();
            group.add(seed);
            remaining.removeIf(other -> {
                boolean matches = other.getDescription().equalsIgnoreCase(seed.getDescription())
                        && other.getTotalAmount().equals(seed.getTotalAmount())
                        && Math.abs(java.time.Duration.between(seed.getExpenseDate(), other.getExpenseDate()).toMinutes()) <= WINDOW_MINUTES;
                if (matches) group.add(other);
                return matches;
            });
            groups.add(group);
        }

        return groups.stream()
                .map(g -> new DuplicateGroupDto(g.stream()
                        .map(e -> new DuplicateGroupDto.FlaggedExpenseDto(
                                e.getId(), e.getDescription(), e.getTotalAmount().cents(),
                                e.getExpenseDate().toString(), e.getCreatedByUserId(), e.isDeleted()))
                        .toList()))
                .toList();
    }

    /**
     * FIN-08: "Unchecking soft-deletes the record from calculations.
     * Re-checking restores it." `keep = false` means the user unchecked
     * this entry (it's a duplicate to discard); `keep = true` restores it.
     * The flag itself is intentionally left on so the dashboard keeps
     * showing the group for reference even after resolution.
     */
    @Transactional
    public Expense resolveDuplicate(UUID expenseId, boolean keep) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + expenseId));
        expense.setDeleted(!keep);
        return expenseRepository.save(expense);
    }
}
