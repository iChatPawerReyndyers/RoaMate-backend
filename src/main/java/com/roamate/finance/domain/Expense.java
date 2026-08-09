package com.roamate.finance.domain;

import com.roamate.common.BaseEntity;
import com.roamate.common.Money;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FIN-01..05: an expense may be paid by multiple sources (Kitty and/or one
 * or more members' personal "abono") and shared by a subset of participants.
 * total amount === sum(payments) is enforced in SettlementService, never
 * assumed.
 */
@Entity
@Table(name = "expenses")
public class Expense extends BaseEntity {

    @Column(nullable = false)
    private UUID tripId;

    @Column(nullable = false)
    private String description;

    @Embedded
    @AttributeOverride(name = "cents", column = @Column(name = "total_amount_cents", nullable = false))
    private Money totalAmount;

    @Column(nullable = false)
    private Instant expenseDate;

    private String category;

    @Column(nullable = false)
    private String createdByUserId;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpensePayment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseParticipant> participants = new ArrayList<>();

    /** Set by DuplicateDetectionService; surfaced in the Conflict Review Dashboard (FIN-08). */
    private boolean flaggedDuplicate = false;

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Money getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Money totalAmount) { this.totalAmount = totalAmount; }
    public Instant getExpenseDate() { return expenseDate; }
    public void setExpenseDate(Instant expenseDate) { this.expenseDate = expenseDate; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }
    public List<ExpensePayment> getPayments() { return payments; }
    public List<ExpenseParticipant> getParticipants() { return participants; }
    public boolean isFlaggedDuplicate() { return flaggedDuplicate; }
    public void setFlaggedDuplicate(boolean flaggedDuplicate) { this.flaggedDuplicate = flaggedDuplicate; }
}
