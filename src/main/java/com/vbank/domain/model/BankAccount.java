package com.vbank.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class BankAccount {
    public enum AccountType {
        CURRENT, SAVINGS
    }

    private final UUID id;
    private final UUID organizationId;
    private final UUID childId;
    private final AccountType accountType;
    private long balance; // in cents, non-negative
    private final String currencyCode;
    private final String accountNumber;
    private boolean isActive;
    private final Instant createdAt;
    private Instant updatedAt;

    @com.fasterxml.jackson.annotation.JsonCreator
    public BankAccount(
            @com.fasterxml.jackson.annotation.JsonProperty("id") UUID id,
            @com.fasterxml.jackson.annotation.JsonProperty("organizationId") UUID organizationId,
            @com.fasterxml.jackson.annotation.JsonProperty("childId") UUID childId,
            @com.fasterxml.jackson.annotation.JsonProperty("accountType") AccountType accountType,
            @com.fasterxml.jackson.annotation.JsonProperty("balance") long balance,
            @com.fasterxml.jackson.annotation.JsonProperty("currencyCode") String currencyCode,
            @com.fasterxml.jackson.annotation.JsonProperty("accountNumber") String accountNumber,
            @com.fasterxml.jackson.annotation.JsonProperty("isActive") boolean isActive,
            @com.fasterxml.jackson.annotation.JsonProperty("createdAt") Instant createdAt,
            @com.fasterxml.jackson.annotation.JsonProperty("updatedAt") Instant updatedAt) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(organizationId, "organizationId");
        Objects.requireNonNull(childId, "childId");
        Objects.requireNonNull(accountType, "accountType");
        Objects.requireNonNull(currencyCode, "currencyCode");
        Objects.requireNonNull(accountNumber, "accountNumber");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");

        if (balance < 0) {
            throw new IllegalArgumentException("balance cannot be negative");
        }
        if (currencyCode.isEmpty()) {
            throw new IllegalArgumentException("currencyCode cannot be empty");
        }
        if (accountNumber.isEmpty()) {
            throw new IllegalArgumentException("accountNumber cannot be empty");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must be >= createdAt");
        }

        this.id = id;
        this.organizationId = organizationId;
        this.childId = childId;
        this.accountType = accountType;
        this.balance = balance;
        this.currencyCode = currencyCode;
        this.accountNumber = accountNumber;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // operations that modify mutable fields
    public synchronized void deposit(long amount, Instant timestamp) {
        if (amount <= 0) {
            throw new IllegalArgumentException("deposit amount must be positive");
        }
        this.balance += amount;
        updateTimestamp(timestamp);
    }

    public synchronized void withdraw(long amount, Instant timestamp) {
        if (amount <= 0) {
            throw new IllegalArgumentException("withdrawal amount must be positive");
        }
        if (accountType == AccountType.SAVINGS) {
            throw new IllegalStateException("cannot withdraw directly from savings account");
        }
        if (balance - amount < 0) {
            throw new IllegalArgumentException("insufficient balance");
        }
        this.balance -= amount;
        updateTimestamp(timestamp);
    }

    private void updateTimestamp(Instant timestamp) {
        Objects.requireNonNull(timestamp, "timestamp");
        if (timestamp.isBefore(this.updatedAt)) {
            throw new IllegalArgumentException("new timestamp must be >= previous updatedAt");
        }
        this.updatedAt = timestamp;
    }

    // getters
    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getChildId() {
        return childId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public long getBalance() {
        return balance;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setActive(boolean active, Instant timestamp) {
        this.isActive = active;
        updateTimestamp(timestamp);
    }

    // equals, hashCode, toString omitted for brevity
}