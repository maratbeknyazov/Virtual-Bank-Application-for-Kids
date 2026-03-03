package com.vbank.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class SavingsGoal {
    private final UUID id;
    private final UUID organizationId;
    private final UUID childId;
    private final UUID savingsAccountId;
    private final String goalName;
    private final long targetAmount; // cents, >0
    private long currentProgress; // cents, >=0
    private final LocalDate deadline;
    private final String category;
    private boolean isCompleted;
    private Instant completedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public SavingsGoal(UUID id,
            UUID organizationId,
            UUID childId,
            UUID savingsAccountId,
            String goalName,
            long targetAmount,
            long currentProgress,
            LocalDate deadline,
            String category,
            boolean isCompleted,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(organizationId, "organizationId");
        Objects.requireNonNull(childId, "childId");
        Objects.requireNonNull(savingsAccountId, "savingsAccountId");
        Objects.requireNonNull(goalName, "goalName");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");

        if (goalName.isEmpty() || goalName.length() > 255) {
            throw new IllegalArgumentException("goalName must be 1-255 chars");
        }
        if (targetAmount <= 0) {
            throw new IllegalArgumentException("targetAmount must be > 0");
        }
        if (currentProgress < 0) {
            throw new IllegalArgumentException("currentProgress cannot be negative");
        }
        if (isCompleted && completedAt == null) {
            throw new IllegalArgumentException("completedAt must be set when goal is completed");
        }
        if (!isCompleted && completedAt != null) {
            throw new IllegalArgumentException("completedAt must be null when goal is not completed");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must be >= createdAt");
        }

        this.id = id;
        this.organizationId = organizationId;
        this.childId = childId;
        this.savingsAccountId = savingsAccountId;
        this.goalName = goalName;
        this.targetAmount = targetAmount;
        this.currentProgress = currentProgress;
        this.deadline = deadline;
        this.category = category;
        this.isCompleted = isCompleted;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // progress update (should only be called by service when savings balance
    // changes)
    public void updateProgress(long newProgress, Instant timestamp) {
        if (newProgress < 0) {
            throw new IllegalArgumentException("progress cannot be negative");
        }
        this.currentProgress = newProgress;
        if (!isCompleted && newProgress >= targetAmount) {
            markCompleted(timestamp);
        }
        updateTimestamp(timestamp);
    }

    private void markCompleted(Instant timestamp) {
        this.isCompleted = true;
        this.completedAt = timestamp;
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

    public UUID getSavingsAccountId() {
        return savingsAccountId;
    }

    public String getGoalName() {
        return goalName;
    }

    public long getTargetAmount() {
        return targetAmount;
    }

    public long getCurrentProgress() {
        return currentProgress;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public String getCategory() {
        return category;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}