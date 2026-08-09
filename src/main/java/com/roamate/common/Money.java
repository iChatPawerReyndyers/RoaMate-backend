package com.roamate.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Money is ALWAYS represented as integer minor units (cents).
 * Per FIN-02 / FIN-05, floating point is never used for currency math
 * anywhere in the system (backend, mobile, or wire format).
 *
 * @JsonValue/@JsonCreator make this serialize as a bare integer (matching
 * every DTO that already exposes cents as a plain long, e.g.
 * SettlementSummary.netDeltaCents). Without these, an entity with an
 * embedded Money field returned directly from a controller - as
 * KittyDeposit is from FinanceController - would serialize as `{}` and
 * fail to deserialize an incoming amount at all, since `cents` is private
 * with no bean-style getter and Money has no Jackson-visible constructor.
 */
@Embeddable
public final class Money implements Serializable, Comparable<Money> {

    public static final Money ZERO = new Money(0L);

    private long cents;

    protected Money() {
        // JPA
    }

    private Money(long cents) {
        this.cents = cents;
    }

    @JsonCreator
    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    public static Money ofDollars(BigDecimal dollars) {
        return new Money(dollars.setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).longValueExact());
    }

    @JsonValue
    public long cents() {
        return cents;
    }

    public BigDecimal toDecimal() {
        return BigDecimal.valueOf(cents, 2);
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(this.cents, other.cents));
    }

    public Money minus(Money other) {
        return new Money(Math.subtractExact(this.cents, other.cents));
    }

    public Money negate() {
        return new Money(Math.negateExact(this.cents));
    }

    public boolean isZero() {
        return cents == 0;
    }

    public boolean isNegative() {
        return cents < 0;
    }

    /**
     * FIN-05 / edge case #3 (Currency Rounding Deltas): splits this amount
     * into `parts` equal base shares, then assigns the ENTIRE leftover
     * remainder (e.g. splitting $100.00 three ways leaves a $0.01 delta) to
     * a single deterministic recipient - per spec, the expense logger
     * (created_by_user_id) - rather than spreading it across multiple
     * participants. `remainderRecipientIndex` is the position of that
     * recipient within the participant list (falls back to index 0, "the
     * first participant listed", if the logger isn't a participant - the
     * spec's stated alternative).
     */
    public Money[] splitEvenlyRemainderToRecipient(int parts, int remainderRecipientIndex) {
        if (parts <= 0) throw new IllegalArgumentException("parts must be > 0");
        if (remainderRecipientIndex < 0 || remainderRecipientIndex >= parts) {
            throw new IllegalArgumentException("remainderRecipientIndex out of range");
        }
        long base = cents / parts;
        long remainder = cents % parts;
        Money[] result = new Money[parts];
        for (int i = 0; i < parts; i++) {
            result[i] = new Money(base);
        }
        result[remainderRecipientIndex] = new Money(base + remainder);
        return result;
    }

    @Override
    public int compareTo(Money o) {
        return Long.compare(this.cents, o.cents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return cents == money.cents;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cents);
    }

    @Override
    public String toString() {
        return toDecimal().toPlainString();
    }
}
