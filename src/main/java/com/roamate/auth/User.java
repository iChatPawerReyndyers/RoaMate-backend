package com.roamate.auth;

import com.roamate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A registered account. This entity's id is what AuthController puts in
 * the JWT `sub` claim once real register/login replaces the old anonymous
 * dev-login flow - it becomes the userId used everywhere else in the
 * schema (TripMember.userId, Expense.createdByUserId, etc.), which have
 * always stored userId as a plain string and don't need to change.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
