package com.roamate.checklist;

import com.roamate.checklist.dto.AssignChecklistItemRequest;
import com.roamate.checklist.dto.SetChecklistItemCategoryRequest;
import com.roamate.finance.domain.Expense;
import com.roamate.finance.dto.CreateExpenseRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checklists")
public class ChecklistController {

    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @PostMapping("/trips/{tripId}/seed-packing-template")
    public List<ChecklistItem> seed(@PathVariable UUID tripId) {
        return checklistService.seedPackingTemplate(tripId);
    }

    @GetMapping("/trips/{tripId}")
    public List<ChecklistItem> list(@PathVariable UUID tripId,
                                    @RequestParam ChecklistCategory category,
                                    @RequestParam String requestingUserId) {
        return checklistService.list(tripId, category, requestingUserId);
    }

    @PostMapping("/items")
    public ChecklistItem addItem(@RequestBody ChecklistItem item) {
        return checklistService.addItem(item);
    }

    @PostMapping("/items/{itemId}/toggle")
    public ChecklistItem toggle(@PathVariable UUID itemId) {
        return checklistService.toggle(itemId);
    }

    /** CHK-05: who's in charge of this shared item (null clears it). */
    @PostMapping("/items/{itemId}/assignee")
    public ChecklistItem assign(@PathVariable UUID itemId, @RequestBody AssignChecklistItemRequest request) {
        return checklistService.assign(itemId, request.assignedToUserId());
    }

    /** CHK-05: move this item to another category / store section (null clears it). */
    @PostMapping("/items/{itemId}/category")
    public ChecklistItem setCategory(@PathVariable UUID itemId, @RequestBody SetChecklistItemCategoryRequest request) {
        return checklistService.setCategory(itemId, request.category());
    }

    @PostMapping("/items/{itemId}/convert-to-expense")
    public Expense convertToExpense(@PathVariable UUID itemId,
                                    @Valid @RequestBody CreateExpenseRequest request) {
        return checklistService.convertToExpense(itemId, request);
    }
}