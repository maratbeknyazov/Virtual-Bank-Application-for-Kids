package com.vbank.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class User {
    public enum Role {
        PARENT, CHILD
    }

    private final UUID id;
    private final UUID organizationId;
    private final String username;
    private final String name;
    private final Role role;
    private final String pinHash;
    private final Integer age; // optional for children
    private final boolean accountLocked;
    private final Instant createdAt;
    private final Instant updatedAt;

    public User(UUID id,
            UUID organizationId,
            String username,
            String name,
            Role role,
            String pinHash,
            Integer age,
            boolean accountLocked,
            Instant createdAt,
            Instant updatedAt) {
        // basic null checks
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(organizationId, "organizationId");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(pinHash, "pinHash");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");

        if (username.isEmpty() || username.length() > 50) {
            throw new IllegalArgumentException("username must be 1-50 characters");
        }
        if (name.isEmpty() || name.length() > 255) {
            throw new IllegalArgumentException("name must be 1-255 characters");
        }
        if (age != null) {
            if (age < 1 || age > 120) {
                throw new IllegalArgumentException("age must be between 1 and 120");
            }
            if (role != Role.CHILD) {
                throw new IllegalArgumentException("only CHILD users may have age set");
            }
        }
        if (pinHash.isEmpty()) {
            throw new IllegalArgumentException("pinHash cannot be empty");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must be >= createdAt");
        }

        this.id = id;
        this.organizationId = organizationId;
        this.username = username;
        this.name = name;
        this.role = role;
        this.pinHash = pinHash;
        this.age = age;
        this.accountLocked = accountLocked;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // getters
    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public String getPinHash() {
        return pinHash;
    }

    public Integer getAge() {
        return age;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // equals, hashCode, toString omitted for brevity
}