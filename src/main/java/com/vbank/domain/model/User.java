package com.vbank.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a user in the Virtual Bank Application for Kids.
 * A user can be either a parent or a child, belonging to an organization.
 * This class is immutable and thread-safe.
 *
 * @author Virtual Bank Team
 * @version 1.0
 * @since 1.0
 */
public final class User {
    /**
     * Enumeration of possible user roles in the system.
     */
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

    /**
     * Constructs a new User instance.
     *
     * @param id             the unique identifier of the user
     * @param organizationId the identifier of the organization this user belongs to
     * @param username       the username for login (1-50 characters)
     * @param name           the display name of the user (1-255 characters)
     * @param role           the role of the user (PARENT or CHILD)
     * @param pinHash        the hashed PIN for authentication
     * @param age            the age of the user (optional, mainly for children)
     * @param accountLocked  whether the account is locked
     * @param createdAt      the timestamp when the user was created
     * @param updatedAt      the timestamp when the user was last updated
     * @throws IllegalArgumentException if any required parameter is null or invalid
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public User(
            @com.fasterxml.jackson.annotation.JsonProperty("id") UUID id,
            @com.fasterxml.jackson.annotation.JsonProperty("organizationId") UUID organizationId,
            @com.fasterxml.jackson.annotation.JsonProperty("username") String username,
            @com.fasterxml.jackson.annotation.JsonProperty("name") String name,
            @com.fasterxml.jackson.annotation.JsonProperty("role") Role role,
            @com.fasterxml.jackson.annotation.JsonProperty("pinHash") String pinHash,
            @com.fasterxml.jackson.annotation.JsonProperty("age") Integer age,
            @com.fasterxml.jackson.annotation.JsonProperty("accountLocked") boolean accountLocked,
            @com.fasterxml.jackson.annotation.JsonProperty("createdAt") Instant createdAt,
            @com.fasterxml.jackson.annotation.JsonProperty("updatedAt") Instant updatedAt) {
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

    /**
     * Returns the unique identifier of this user.
     *
     * @return the user's ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the identifier of the organization this user belongs to.
     *
     * @return the organization ID
     */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /**
     * Returns the username used for login.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the display name of the user.
     *
     * @return the display name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the role of this user.
     *
     * @return the user role (PARENT or CHILD)
     */
    public Role getRole() {
        return role;
    }

    /**
     * Returns the hashed PIN for authentication.
     *
     * @return the PIN hash
     */
    public String getPinHash() {
        return pinHash;
    }

    /**
     * Returns the age of the user (only applicable for children).
     *
     * @return the age, or null if not set
     */
    public Integer getAge() {
        return age;
    }

    /**
     * Checks if the user account is locked.
     *
     * @return true if the account is locked, false otherwise
     */
    public boolean isAccountLocked() {
        return accountLocked;
    }

    /**
     * Returns the timestamp when this user was created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the timestamp when this user was last updated.
     *
     * @return the last update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof User user))
            return false;
        return accountLocked == user.accountLocked &&
                Objects.equals(id, user.id) &&
                Objects.equals(organizationId, user.organizationId) &&
                Objects.equals(username, user.username) &&
                Objects.equals(name, user.name) &&
                role == user.role &&
                Objects.equals(pinHash, user.pinHash) &&
                Objects.equals(age, user.age) &&
                Objects.equals(createdAt, user.createdAt) &&
                Objects.equals(updatedAt, user.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, organizationId, username, name, role, pinHash, age, accountLocked, createdAt,
                updatedAt);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", organizationId=" + organizationId +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", role=" + role +
                ", age=" + age +
                ", accountLocked=" + accountLocked +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}