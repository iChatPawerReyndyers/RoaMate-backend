package com.roamate.finance.domain;

import com.roamate.common.BaseEntity;
import com.roamate.common.Money;
import jakarta.persistence.*;

/**
 * One "who paid how much" row. A single expense can have N of these -
 * e.g. Kitty pays $30, Alice abonos $20 - and they must sum to the
 * expense total (validated in SettlementService).
 */
@Entity
@Table(name = "expense_payments")
public class ExpensePayment extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentSource source;

    /** Null when source == KITTY. */
    private String payerUserId;

    @Embedded
    @AttributeOverride(name = "cents", column = @Column(name = "amount_paid_cents", nullable = false))
    private Money amountPaid;

    public Expense getExpense() { return expense; }
    public void setExpense(Expense expense) { this.expense = expense; }
    public PaymentSource getSource() { return source; }
    public void setSource(PaymentSource source) { this.source = source; }
    public String getPayerUserId() { return payerUserId; }
    public void setPayerUserId(String payerUserId) { this.payerUserId = payerUserId; }
    public Money getAmountPaid() { return amountPaid; }
    public void setAmountPaid(Money amountPaid) { this.amountPaid = amountPaid; }
}
