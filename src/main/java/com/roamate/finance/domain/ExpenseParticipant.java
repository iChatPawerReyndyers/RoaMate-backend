package com.roamate.finance.domain;

import com.roamate.common.BaseEntity;
import com.roamate.common.Money;
import jakarta.persistence.*;

/**
 * FIN-04: granular per-person inclusion in an expense's cost share, with
 * the computed "fair share" cached at settlement time.
 */
@Entity
@Table(name = "expense_participants")
public class ExpenseParticipant extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @Column(nullable = false)
    private String userId;

    @Embedded
    @AttributeOverride(name = "cents", column = @Column(name = "fair_share_cents", nullable = false))
    private Money fairShare;

    public Expense getExpense() { return expense; }
    public void setExpense(Expense expense) { this.expense = expense; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Money getFairShare() { return fairShare; }
    public void setFairShare(Money fairShare) { this.fairShare = fairShare; }
}
