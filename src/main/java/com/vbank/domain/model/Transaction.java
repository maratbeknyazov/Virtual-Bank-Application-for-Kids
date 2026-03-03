package com.vbank.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Transaction {
    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER
    }

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

    public Transaction(UUID id,
            UUID organizationId,
            UUID accountId,
            TransactionType transactionType,
            long amount,
            long balanceAfter,
            String description,
            String category,
            UUID relatedTransactionId,
            Status status,
            Instant createdAt,
            Instant updatedAt) {
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
    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public long getAmount() {
        return amount;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

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