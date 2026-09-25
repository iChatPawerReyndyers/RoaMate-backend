package com.roamate.checklist;

import com.roamate.finance.SettlementService;
import com.roamate.finance.domain.Expense;
import com.roamate.finance.dto.CreateExpenseRequest;
import com.roamate.trip.TripMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** CHK-01/02: seeds a trip with a starter template, then supports free editing. */
@Service
public class ChecklistService {

    private static final List<String> DEFAULT_PACKING_TEMPLATE = List.of(
            "Passport / ID", "Phone charger", "First-aid kit", "Reusable water bottle", "Weather-appropriate layers"
    );

    private final ChecklistRepository repository;
    private final SettlementService settlementService;
    private final TripMemberRepository tripMemberRepository;

    public ChecklistService(ChecklistRepository repository,
                            SettlementService settlementService,
                            TripMemberRepository tripMemberRepository) {
        this.repository = repository;
        this.settlementService = settlementService;
        this.tripMemberRepository = tripMemberRepository;
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
     * CHK-01/06: SHARED and EVERYONE items are visible to everyone; PERSONAL
     * items are only returned to their own owner, never to other trip members.
     */
    public List<ChecklistItem> list(UUID tripId, ChecklistCategory category, String requestingUserId) {
        return repository.findByTripIdAndCategoryAndDeletedFalse(tripId, category).stream()
                .filter(item -> item.getVisibility() == ChecklistVisibility.SHARED
                        || item.getVisibility() == ChecklistVisibility.EVERYONE
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

    /**
     * CHK-05: put a trip member in charge of a SHARED item (null/blank clears it).
     * PERSONAL items can't be assigned - they belong to one person and other
     * members never see them. EVERYONE items can't be assigned either - by
     * definition every member brings their own, so there's no single person
     * to put in charge. Either way the assignee has to be on the item's trip.
     */
    @Transactional
    public ChecklistItem assign(UUID itemId, String assignedToUserId) {
        ChecklistItem item = repository.findById(itemId).orElseThrow();
        if (item.getVisibility() != ChecklistVisibility.SHARED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only shared checklist items can be assigned");
        }
        String assignee = (assignedToUserId == null || assignedToUserId.isBlank()) ? null : assignedToUserId.trim();
        if (assignee != null && tripMemberRepository.findByTripIdAndUserId(item.getTripId(), assignee).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee is not a member of this trip");
        }
        item.setAssignedToUserId(assignee);
        return repository.save(item);
    }

    /**
     * CHK-05: change an existing item's category. PACKING items take a
     * PackingItemCategory name; every other kind of list uses the free-text
     * store section. null/blank clears it.
     */
    @Transactional
    public ChecklistItem setCategory(UUID itemId, String category) {
        ChecklistItem item = repository.findById(itemId).orElseThrow();
        String value = (category == null || category.isBlank()) ? null : category.trim();
        if (item.getCategory() == ChecklistCategory.PACKING) {
            if (value == null) {
                item.setPackingItemCategory(null);
            } else {
                try {
                    item.setPackingItemCategory(PackingItemCategory.valueOf(value.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown packing category: " + value);
                }
            }
        } else {
            item.setStoreCategory(value);
        }
        return repository.save(item);
    }
}