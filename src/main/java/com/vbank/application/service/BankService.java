package com.vbank.application.service;

import com.vbank.domain.model.BankAccount;
import com.vbank.domain.model.User;
import com.vbank.domain.repository.Repository;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service coordinating bank account operations.
 * <p>
 * Only a very small portion of the banking logic lives here – the heavy
 * lifting is done by the domain objects. The service performs lookups,
 * enforces simple invariants and orchestrates use cases such as transfers
 * between the two accounts belonging to a child.
 */
public class BankService {
    private final Repository<BankAccount> accountRepo;
    private final Repository<User> userRepo;

    public BankService(Repository<BankAccount> accountRepo,
            Repository<User> userRepo) {
        this.accountRepo = accountRepo;
        this.userRepo = userRepo;
    }

    /**
     * Verifies that the provided plain‑text PIN matches the stored hash for the
     * given user. Throws an exception if the user does not exist.
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
