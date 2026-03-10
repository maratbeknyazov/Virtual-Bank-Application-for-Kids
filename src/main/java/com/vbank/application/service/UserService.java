package com.vbank.application.service;

import com.vbank.domain.model.User;
import com.vbank.domain.repository.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Application service providing user management operations in the Virtual Bank
 * Application for Kids.
 * <p>
 * This service handles user-related queries and operations, including finding
 * users by ID,
 * retrieving children and parents within organizations, and managing user data
 * access.
 * It serves as an interface between the presentation layer and the user
 * repository.
 * </p>
 *
 * @author Virtual Bank Team
 * @version 1.0
 * @since 1.0
 */
public class UserService {
    private final Repository<User> userRepo;

    /**
     * Constructs a new UserService with the user repository.
     *
     * @param userRepo the repository for user operations
     * @throws IllegalArgumentException if userRepo is null
     */
    public UserService(Repository<User> userRepo) {
        if (userRepo == null) {
            throw new IllegalArgumentException("userRepo cannot be null");
        }
        this.userRepo = userRepo;
    }

    /**
     * Retrieves all children belonging to the specified organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of child users in the organization
     */
    public List<User> getAllChildren(UUID organizationId) {
        return userRepo.findAll().stream()
                .filter(u -> u.getOrganizationId().equals(organizationId) && u.getRole() == User.Role.CHILD)
                .toList();
    }

    /**
     * Convenience variant used by the dashboard when there is no specific org
     * context (e.g. during early prototype/demo).
     * <p>
     * Retrieves all children across all organizations. Use with caution in
     * production.
     * </p>
     *
     * @return a list of all child users in the system
     */
    public List<User> getAllChildren() {
        return userRepo.findAll().stream()
                .filter(u -> u.getRole() == User.Role.CHILD)
                .toList();
    }

    /**
     * Retrieves all parents belonging to the specified organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of parent users in the organization
     */
    public List<User> getAllParents(UUID organizationId) {
        return userRepo.findAll().stream()
                .filter(u -> u.getOrganizationId().equals(organizationId) && u.getRole() == User.Role.PARENT)
                .toList();
    }

    /**
     * Finds a user by their unique identifier.
     *
     * @param id the unique identifier of the user
     * @return the user if found, null otherwise
     */
    public User findById(UUID id) {
        return userRepo.findById(id).orElse(null);
    }
}
