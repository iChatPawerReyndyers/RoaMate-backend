package com.roamate.finance.repo;

import com.roamate.finance.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByTripIdAndDeletedFalse(UUID tripId);

    List<Expense> findByTripIdAndDescriptionIgnoreCaseAndExpenseDateBetweenAndDeletedFalse(
            UUID tripId, String description, Instant from, Instant to);

    /** FIN-08: everything currently sitting in the Conflict Review Dashboard for a trip. */
    List<Expense> findByTripIdAndFlaggedDuplicateTrue(UUID tripId);
}
