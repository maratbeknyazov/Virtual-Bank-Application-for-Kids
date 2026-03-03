package com.vbank.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Task {
    public enum Status {
        OPEN, SUBMITTED, APPROVED, REJECTED, COMPLETED, EXPIRED
    }

    private final UUID id;
    private final UUID organizationId;
    private final UUID parentId;
    private final UUID childId;
    private final String taskDescription;
    private final long rewardAmount; // cents, >0
    private Status taskStatus;
    private final String category;
    private final LocalDate deadline;
    private Instant submittedAt;
    private Instant approvedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public Task(UUID id,
            UUID organizationId,
            UUID parentId,
            UUID childId,
            String taskDescription,
            long rewardAmount,
            Status taskStatus,
            String category,
            LocalDate deadline,
            Instant submittedAt,
            Instant approvedAt,
            Instant createdAt,
            Instant updatedAt) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(organizationId, "organizationId");
        Objects.requireNonNull(parentId, "parentId");
        Objects.requireNonNull(childId, "childId");
        Objects.requireNonNull(taskDescription, "taskDescription");
        Objects.requireNonNull(taskStatus, "taskStatus");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");

        if (taskDescription.isEmpty() || taskDescription.length() > 500) {
            throw new IllegalArgumentException("taskDescription must be 1-500 chars");
        }
        if (rewardAmount <= 0) {
            throw new IllegalArgumentException("rewardAmount must be > 0");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must be >= createdAt");
        }
        // status-based timestamps validity
        if (taskStatus == Status.SUBMITTED && submittedAt == null) {
            throw new IllegalArgumentException("submittedAt required when status is SUBMITTED");
        }
        if ((taskStatus == Status.APPROVED || taskStatus == Status.COMPLETED) && approvedAt == null) {
            throw new IllegalArgumentException("approvedAt required when status is APPROVED/COMPLETED");
        }

        this.id = id;
        this.organizationId = organizationId;
        this.parentId = parentId;
        this.childId = childId;
        this.taskDescription = taskDescription;
        this.rewardAmount = rewardAmount;
        this.taskStatus = taskStatus;
        this.category = category;
        this.deadline = deadline;
        this.submittedAt = submittedAt;
        this.approvedAt = approvedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // state transition helpers
    public void submit(Instant timestamp) {
        if (taskStatus != Status.OPEN) {
            throw new IllegalStateException("can only submit task in OPEN status");
        }
        this.taskStatus = Status.SUBMITTED;
        this.submittedAt = timestamp;
        updateTimestamp(timestamp);
    }

    public void approve(Instant timestamp) {
        if (taskStatus != Status.OPEN && taskStatus != Status.SUBMITTED) {
            throw new IllegalStateException("can only approve OPEN or SUBMITTED task");
        }
        this.taskStatus = Status.APPROVED;
        this.approvedAt = timestamp;
        updateTimestamp(timestamp);
    }

    public void complete(Instant timestamp) {
        if (taskStatus != Status.APPROVED) {
            throw new IllegalStateException("can only complete an approved task");
        }
        this.taskStatus = Status.COMPLETED;
        // approvedAt should already be set
        updateTimestamp(timestamp);
    }

    public void reject(Instant timestamp) {
        if (taskStatus == Status.SUBMITTED || taskStatus == Status.OPEN) {
            this.taskStatus = Status.REJECTED;
            updateTimestamp(timestamp);
        } else {
            throw new IllegalStateException("can only reject OPEN or SUBMITTED task");
        }
    }

    public void expire(Instant timestamp) {
        if (taskStatus == Status.OPEN || taskStatus == Status.SUBMITTED) {
            this.taskStatus = Status.EXPIRED;
            updateTimestamp(timestamp);
        } else {
            throw new IllegalStateException("only OPEN or SUBMITTED tasks can expire");
        }
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

    public UUID getParentId() {
        return parentId;
    }

    public UUID getChildId() {
        return childId;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public long getRewardAmount() {
        return rewardAmount;
    }

    public Status getTaskStatus() {
        return taskStatus;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}