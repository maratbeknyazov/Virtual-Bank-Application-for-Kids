package com.vbank.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a financial transaction in the Virtual Bank Application for Kids.
 * Transactions can be deposits, withdrawals, or transfers between accounts.
 * This class is immutable and thread-safe.
 *
 * @author Virtual Bank Team
 * @version 1.0
 * @since 1.0
 */
public final class Transaction {
    /**
     * Enumeration of possible transaction types.
     */
    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER
    }

    /**
     * Enumeration of possible transaction statuses.
     */
    public enum Status {
        PENDING, COMPLETED, FAILED, REJECTED
    }

    private final UUID id;
    private final UUID organizationId;
    private final UUID accountId;
    private final TransactionType transactionType;
    private final long amount; // in cents, always >0
    private final long balanceAfter; // snapshot at time of creation, >=0
    private final String description;
    private final String category;
    private final UUID relatedTransactionId;
    private final Status status;
    private final Instant createdAt;
    private final Instant updatedAt;

    /**
     * Constructs a new Transaction instance.
     *
     * @param id                   the unique identifier of the transaction
     * @param organizationId       the identifier of the organization
     * @param accountId            the identifier of the account this transaction
     *                             belongs to
     * @param transactionType      the type of transaction (DEPOSIT, WITHDRAWAL, or
     *                             TRANSFER)
     * @param amount               the transaction amount in cents (must be
     *                             positive)
     * @param balanceAfter         the account balance after this transaction
     * @param description          a description of the transaction
     * @param category             the category of the transaction
     * @param relatedTransactionId the ID of a related transaction (for transfers)
     * @param status               the status of the transaction
     * @param createdAt            the timestamp when the transaction was created
     * @param updatedAt            the timestamp when the transaction was last
     *                             updated
     * @throws IllegalArgumentException if any required parameter is null or invalid
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public Transaction(
            @com.fasterxml.jackson.annotation.JsonProperty("id") UUID id,
            @com.fasterxml.jackson.annotation.JsonProperty("organizationId") UUID organizationId,
            @com.fasterxml.jackson.annotation.JsonProperty("accountId") UUID accountId,
            @com.fasterxml.jackson.annotation.JsonProperty("transactionType") TransactionType transactionType,
            @com.fasterxml.jackson.annotation.JsonProperty("amount") long amount,
            @com.fasterxml.jackson.annotation.JsonProperty("balanceAfter") long balanceAfter,
            @com.fasterxml.jackson.annotation.JsonProperty("description") String description,
            @com.fasterxml.jackson.annotation.JsonProperty("category") String category,
            @com.fasterxml.jackson.annotation.JsonProperty("relatedTransactionId") UUID relatedTransactionId,
            @com.fasterxml.jackson.annotation.JsonProperty("status") Status status,
            @com.fasterxml.jackson.annotation.JsonProperty("createdAt") Instant createdAt,
            @com.fasterxml.jackson.annotation.JsonProperty("updatedAt") Instant updatedAt) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(organizationId, "organizationId");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(transactionType, "transactionType");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");

        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (balanceAfter < 0) {
            throw new IllegalArgumentException("balanceAfter cannot be negative");
        }
        if (description != null && description.length() > 255) {
            throw new IllegalArgumentException("description max 255 chars");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must be >= createdAt");
        }

        this.id = id;
        this.organizationId = organizationId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.category = category;
        this.relatedTransactionId = relatedTransactionId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // since transactions are immutable, no setters are provided

    /**
     * Creates a new Transaction instance with an updated status.
     * This method allows changing the status of a pending transaction to completed
     * or failed.
     *
     * @param newStatus the new status for the transaction
     * @param updatedAt the timestamp of the status update
     * @return a new Transaction instance with the updated status
     * @throws IllegalArgumentException if newStatus is null or updatedAt is invalid
     */
    public Transaction withStatus(Status newStatus, Instant updatedAt) {
        // allow changing status for pending→completed/failed, but not other fields
        if (newStatus == null) {
            throw new IllegalArgumentException("newStatus cannot be null");
        }
        if (updatedAt.isBefore(this.updatedAt)) {
            throw new IllegalArgumentException("updatedAt must be >= current updatedAt");
        }
        return new Transaction(this.id, this.organizationId, this.accountId, this.transactionType,
                this.amount, this.balanceAfter, this.description, this.category,
                this.relatedTransactionId, newStatus, this.createdAt, updatedAt);
    }

    // getters

    /**
     * Returns the unique identifier of this transaction.
     *
     * @return the transaction ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the identifier of the organization this transaction belongs to.
     *
     * @return the organization ID
     */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /**
     * Returns the identifier of the account this transaction belongs to.
     *
     * @return the account ID
     */
    public UUID getAccountId() {
        return accountId;
    }

    /**
     * Returns the type of this transaction.
     *
     * @return the transaction type (DEPOSIT, WITHDRAWAL, or TRANSFER)
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Returns the amount of this transaction.
     *
     * @return the amount in cents
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Returns the account balance after this transaction was executed.
     *
     * @return the balance after transaction in cents
     */
    public long getBalanceAfter() {
        return balanceAfter;
    }

    /**
     * Returns the description of this transaction.
     *
     * @return the transaction description, or null if not provided
     */
    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public UUID getRelatedTransactionId() {
        return relatedTransactionId;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}