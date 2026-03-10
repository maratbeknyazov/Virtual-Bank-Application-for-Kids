package com.vbank.application.service;

import com.vbank.domain.model.BankAccount;
import com.vbank.domain.model.User;
import com.vbank.domain.repository.Repository;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service coordinating bank account operations in the Virtual Bank Application for Kids.
 * <p>
 * This service handles banking operations such as PIN verification, deposits, withdrawals,
 * and transfers between accounts. It orchestrates domain objects and enforces business rules
 * while keeping the domain logic within the domain models themselves.
 * </p>
 *
 * <p>The service performs the following key functions:</p>
 * <ul>
 *   <li>PIN verification for user authentication</li>
 *   <li>Deposit operations to accounts</li>
 *   <li>Withdrawal operations from current accounts</li>
 *   <li>Transfer operations between a child's current and savings accounts</li>
 *   <li>Account balance inquiries</li>
 * </ul>
 *
 * @author Virtual Bank Team
 * @version 1.0
 * @since 1.0
 */
public class BankService {
    private final Repository<BankAccount> accountRepo;
    private final Repository<User> userRepo;

    /**
     * Constructs a new BankService with the required repositories.
     *
     * @param accountRepo the repository for bank account operations
     * @param userRepo the repository for user operations
     * @throws IllegalArgumentException if either repository is null
     */
    public BankService(Repository<BankAccount> accountRepo,
            Repository<User> userRepo) {
        if (accountRepo == null) {
            throw new IllegalArgumentException("accountRepo cannot be null");
        }
        if (userRepo == null) {
            throw new IllegalArgumentException("userRepo cannot be null");
        }
        this.accountRepo = accountRepo;
        this.userRepo = userRepo;
    }

    /**
     * Verifies that the provided plain-text PIN matches the stored hash for the
     * given user. Throws an exception if the user does not exist.
     *
     * @param userId the unique identifier of the user
     * @param pin the plain-text PIN to verify
     * @return true if the PIN matches, false otherwise
     * @throws IllegalArgumentException if the user does not exist
     */
    public boolean checkPin(UUID userId, String pin) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (pin == null) {
            return false;
        }
        return BCrypt.checkpw(pin, u.getPinHash());
    }

    /**
     * Transfer funds from a child's current account into the matching savings
     * account. A valid parent PIN must be supplied to authorise the action.
     *
     * <p>
     * The method looks up the two accounts by the child's id and account
     * type, performs basic sanity checks and persists the modified entities.
     * </p>
     *
     * @param parentId the ID of the parent authorizing the transfer
     * @param childId the ID of the child whose accounts are involved
     * @param amount the amount to transfer in cents (must be positive)
     * @param parentPin the parent's PIN for authorization
     * @throws IllegalArgumentException if validation fails or accounts not found
     * @throws IllegalStateException if PIN verification fails
     */
    public void transferToSavings(UUID parentId,
            String parentPin,
            UUID childId,
            long amount) {
        if (!checkPin(parentId, parentPin)) {
            throw new SecurityException("invalid PIN");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("transfer amount must be positive");
        }

        BankAccount current = findAccount(childId, BankAccount.AccountType.CURRENT);
        BankAccount savings = findAccount(childId, BankAccount.AccountType.SAVINGS);

        if (current.getBalance() < amount) {
            throw new IllegalStateException("insufficient balance");
        }

        Instant now = Instant.now();
        current.withdraw(amount, now);
        savings.deposit(amount, now);

        accountRepo.save(current);
        accountRepo.save(savings);
    }

    /**
     * Utility used by other services (e.g. {@link TaskService}) to credit money
     * to a child's current account. No PIN check is performed because the
     * caller is assumed to have already done the necessary authorisation.
     */
    public void depositToCurrent(UUID childId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("deposit amount must be positive");
        }
        BankAccount current = findAccount(childId, BankAccount.AccountType.CURRENT);
        current.deposit(amount, Instant.now());
        accountRepo.save(current);
    }

    private BankAccount findAccount(UUID childId, BankAccount.AccountType type) {
        return accountRepo.findAll().stream()
                .filter(a -> a.getChildId().equals(childId) && a.getAccountType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("account not found"));
    }
}
