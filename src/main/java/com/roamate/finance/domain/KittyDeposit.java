package com.roamate.finance.domain;

import com.roamate.common.BaseEntity;
import com.roamate.common.Money;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** FIN-06: a member contributing cash into the shared group kitty. */
@Entity
@Table(name = "kitty_deposits")
public class KittyDeposit extends BaseEntity {

    @Column(nullable = false)
    private UUID tripId;

    @Column(nullable = false)
    private String depositorUserId;

    @Embedded
    @AttributeOverride(name = "cents", column = @Column(name = "amount_cents", nullable = false))
    private Money amount;

    @Column(nullable = false)
    private Instant depositedAt;

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }
    public String getDepositorUserId() { return depositorUserId; }
    public void setDepositorUserId(String depositorUserId) { this.depositorUserId = depositorUserId; }
    public Money getAmount() { return amount; }
    public void setAmount(Money amount) { this.amount = amount; }
    public Instant getDepositedAt() { return depositedAt; }
    public void setDepositedAt(Instant depositedAt) { this.depositedAt = depositedAt; }
}
