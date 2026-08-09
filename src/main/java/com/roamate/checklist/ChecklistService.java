package com.roamate.checklist;

import com.roamate.finance.SettlementService;
import com.roamate.finance.domain.Expense;
import com.roamate.finance.dto.CreateExpenseRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** CHK-01/02: seeds a trip with a starter template, then supports free editing. */
@Service
public class ChecklistService {

    private static final List<String> DEFAULT_PACKING_TEMPLATE = List.of(
            "Passport / ID", "Phone charger", "First-aid kit", "Reusable water bottle", "Weather-appropriate layers"
    );

    private final ChecklistRepository repository;
    private final SettlementService settlementService;

    public ChecklistService(ChecklistRepository repository, SettlementService settlementService) {
        this.repository = repository;
        this.settlementService = settlementService;
    }

    @Transactional
    public Expense convertToExpense(UUID itemId, CreateExpenseRequest request) {
        ChecklistItem item = repository.findById(itemId).orElseThrow();
        if (item.getConvertedExpenseId() != null) {
            throw new IllegalStateException("Checklist item has already been converted to an expense");
        }
        if (!item.getTripId().equals(request.tripId())) {
            throw new IllegalArgumentException("Trip mismatch between checklist item and expense request");
        }

        Expense expense = settlementService.createExpense(request);
        item.setConvertedExpenseId(expense.getId());
        repository.save(item);
        return expense;
    }

    public List<ChecklistItem> seedPackingTemplate(UUID tripId) {
        return DEFAULT_PACKING_TEMPLATE.stream().map(label -> {
            ChecklistItem item = new ChecklistItem();
            item.setTripId(tripId);
            item.setCategory(ChecklistCategory.PACKING);
            item.setLabel(label);
            return repository.save(item);
        }).toList();
    }

    /**
     * CHK-01: SHARED items are visible to everyone; PERSONAL items are only
     * returned to their own owner, never to other trip members.
     */
    public List<ChecklistItem> list(UUID tripId, ChecklistCategory category, String requestingUserId) {
        return repository.findByTripIdAndCategoryAndDeletedFalse(tripId, category).stream()
                .filter(item -> item.getVisibility() == ChecklistVisibility.SHARED
                        || requestingUserId.equals(item.getOwnerUserId()))
                .toList();
    }

    @Transactional
    public ChecklistItem toggle(UUID itemId) {
        ChecklistItem item = repository.findById(itemId).orElseThrow();
        item.setChecked(!item.isChecked());
        return repository.save(item);
    }

    @Transactional
    public ChecklistItem addItem(ChecklistItem item) {
        return repository.save(item);
    }
}
