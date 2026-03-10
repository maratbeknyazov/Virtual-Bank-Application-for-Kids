package com.vbank.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a bank account in the Virtual Bank Application for Kids.
 * Each child can have multiple accounts (current and savings).
 * This class manages account balance and transaction operations.
 *
 * @author Virtual Bank Team
 * @version 1.0
 * @since 1.0
 */
public final class BankAccount {
    /**
     * Enumeration of possible account types.
     */
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

    /**
     * Constructs a new BankAccount instance.
     *
     * @param id             the unique identifier of the account
     * @param organizationId the identifier of the organization
     * @param childId        the identifier of the child who owns this account
     * @param accountType    the type of account (CURRENT or SAVINGS)
     * @param balance        the current balance in cents (must be non-negative)
     * @param currencyCode   the currency code (e.g., "USD")
     * @param accountNumber  the account number
     * @param isActive       whether the account is active
     * @param createdAt      the timestamp when the account was created
     * @param updatedAt      the timestamp when the account was last updated
     * @throws IllegalArgumentException if any required parameter is null or invalid
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public BankAccount(
            @com.fasterxml.jackson.annotation.JsonProperty("id") UUID id,
            @com.fasterxml.jackson.annotation.JsonProperty("organizationId") UUID organizationId,
            @com.fasterxml.jackson.annotation.JsonProperty("childId") UUID childId,
            @com.fasterxml.jackson.annotation.JsonProperty("accountType") AccountType accountType,
            @com.fasterxml.jackson.annotation.JsonProperty("balance") long balance,
            @com.fasterxml.jackson.annotation.JsonProperty("currencyCode") String currencyCode,
            @com.fasterxml.jackson.annotation.JsonProperty("accountNumber") String accountNumber,
            @com.fasterxml.jackson.annotation.JsonProperty("active") boolean isActive,
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

    /**
     * Deposits the specified amount into this account.
     * This operation is thread-safe.
     *
     * @param amount    the amount to deposit in cents (must be positive)
     * @param timestamp the timestamp of the operation
     * @throws IllegalArgumentException if amount is not positive or timestamp is
     *                                  invalid
     */
    public synchronized void deposit(long amount, Instant timestamp) {
        if (amount <= 0) {
            throw new IllegalArgumentException("deposit amount must be positive");
        }
        this.balance += amount;
        updateTimestamp(timestamp);
    }

    /**
     * Withdraws the specified amount from this account.
     * Only available for CURRENT accounts. This operation is thread-safe.
     *
     * @param amount    the amount to withdraw in cents (must be positive)
     * @param timestamp the timestamp of the operation
     * @throws IllegalArgumentException if amount is not positive, insufficient
     *                                  balance, or invalid timestamp
     * @throws IllegalStateException    if attempting to withdraw from a SAVINGS
     *                                  account
     */
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

    /**
     * Updates the last modified timestamp of this account.
     *
     * @param timestamp the new timestamp
     * @throws IllegalArgumentException if timestamp is null or before current
     *                                  updatedAt
     */
    private void updateTimestamp(Instant timestamp) {
        Objects.requireNonNull(timestamp, "timestamp");
        if (timestamp.isBefore(this.updatedAt)) {
            throw new IllegalArgumentException("new timestamp must be >= previous updatedAt");
        }
        this.updatedAt = timestamp;
    }

    // getters

    /**
     * Returns the unique identifier of this account.
     *
     * @return the account ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the identifier of the organization this account belongs to.
     *
     * @return the organization ID
     */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /**
     * Returns the identifier of the child who owns this account.
     *
     * @return the child ID
     */
    public UUID getChildId() {
        return childId;
    }

    /**
     * Returns the type of this account.
     *
     * @return the account type (CURRENT or SAVINGS)
     */
    public AccountType getAccountType() {
        return accountType;
    }

    /**
     * Returns the current balance of this account.
     *
     * @return the balance in cents
     */
    public long getBalance() {
        return balance;
    }

    /**
     * Returns the currency code of this account.
     *
     * @return the currency code (e.g., "USD")
     */
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