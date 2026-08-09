package com.roamate.checklist;

import com.roamate.common.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

/** CHK-01..04: a single packing/grocery checklist item, optionally converted to an expense. */
@Entity
@Table(name = "checklist_items")
public class ChecklistItem extends BaseEntity {

    @Column(nullable = false)
    private UUID tripId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChecklistCategory category;

    @Column(nullable = false)
    private String label;

    private boolean checked = false;

    private String assignedToUserId;

    /** CHK-01: PERSONAL items are visible only to ownerUserId's own device/account; SHARED items sync to the group. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChecklistVisibility visibility = ChecklistVisibility.SHARED;

    /** Required when visibility == PERSONAL; who this item belongs to. */
    private String ownerUserId;

    /** CHK-03: grocery-specific fields. Null/unused for PACKING items. */
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private ChecklistPriority priority;

    private String storeCategory;

    /** CHK-04: set once this item has been converted into a linked Expense. */
    private UUID convertedExpenseId;

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }
    public ChecklistCategory getCategory() { return category; }
    public void setCategory(ChecklistCategory category) { this.category = category; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }
    public String getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(String assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    public ChecklistVisibility getVisibility() { return visibility; }
    public void setVisibility(ChecklistVisibility visibility) { this.visibility = visibility; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public ChecklistPriority getPriority() { return priority; }
    public void setPriority(ChecklistPriority priority) { this.priority = priority; }
    public String getStoreCategory() { return storeCategory; }
    public void setStoreCategory(String storeCategory) { this.storeCategory = storeCategory; }
    public UUID getConvertedExpenseId() { return convertedExpenseId; }
    public void setConvertedExpenseId(UUID convertedExpenseId) { this.convertedExpenseId = convertedExpenseId; }
}
