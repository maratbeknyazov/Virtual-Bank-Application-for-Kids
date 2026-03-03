package com.vbank.application.service;

import com.vbank.domain.model.BankAccount;
import com.vbank.domain.model.User;
import com.vbank.infrastructure.persistence.JsonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BankServiceTest {
    private Path dataDir;
    private JsonRepository<BankAccount> accountRepo;
    private JsonRepository<User> userRepo;
    private BankService bankService;

    @BeforeEach
    void setUp() throws IOException {
        dataDir = Path.of("data");
        if (Files.exists(dataDir)) {
            deleteRecursively(dataDir);
        }
        accountRepo = new JsonRepository<>(BankAccount.class);
        userRepo = new JsonRepository<>(User.class);
        bankService = new BankService(accountRepo, userRepo);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(dataDir)) {
            deleteRecursively(dataDir);
        }
    }

    private void deleteRecursively(Path p) throws IOException {
        if (Files.isDirectory(p)) {
            try (var stream = Files.list(p)) {
                stream.forEach(path -> {
                    try {
                        deleteRecursively(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        Files.deleteIfExists(p);
    }

    @Test
    void checkPin_validAndInvalid() {
        UUID id = UUID.randomUUID();
        String rawPin = "1234";
        String hash = BCrypt.hashpw(rawPin, BCrypt.gensalt());
        User parent = new User(id,
                UUID.randomUUID(),
                "parent",
                "Parent",
                User.Role.PARENT,
                hash,
                null,
                false,
                Instant.now(),
                Instant.now());
        userRepo.save(parent);

        assertTrue(bankService.checkPin(id, rawPin));
        assertFalse(bankService.checkPin(id, "0000"));
    }

    @Test
    void transferToSavings_happyPath() {
        UUID parentId = UUID.randomUUID();
        String parentPin = "0000";
        String hash = BCrypt.hashpw(parentPin, BCrypt.gensalt());
        User p = new User(parentId,
                UUID.randomUUID(),
                "john",
                "John",
                User.Role.PARENT,
                hash,
                null,
                false,
                Instant.now(),
                Instant.now());
        userRepo.save(p);

        UUID childId = UUID.randomUUID();
        BankAccount current = new BankAccount(UUID.randomUUID(),
                p.getOrganizationId(),
                childId,
                BankAccount.AccountType.CURRENT,
                10000,
                "USD",
                "C-001",
                true,
                Instant.now(),
                Instant.now());
        BankAccount savings = new BankAccount(UUID.randomUUID(),
                p.getOrganizationId(),
                childId,
                BankAccount.AccountType.SAVINGS,
                2000,
                "USD",
                "S-001",
                true,
                Instant.now(),
                Instant.now());
        accountRepo.save(current);
        accountRepo.save(savings);

        bankService.transferToSavings(parentId, parentPin, childId, 3000);

        BankAccount afterCurrent = accountRepo.findById(current.getId()).orElseThrow();
        BankAccount afterSavings = accountRepo.findById(savings.getId()).orElseThrow();
        assertEquals(7000, afterCurrent.getBalance());
        assertEquals(5000, afterSavings.getBalance());
    }

    @Test
    void transferToSavings_failsWithWrongPinOrInsufficientFunds() {
        UUID parentId = UUID.randomUUID();
        String parentPin = "9999";
        String hash = BCrypt.hashpw(parentPin, BCrypt.gensalt());
        User p = new User(parentId,
                UUID.randomUUID(),
                "alice",
                "Alice",
                User.Role.PARENT,
                hash,
                null,
                false,
                Instant.now(),
                Instant.now());
        userRepo.save(p);

        UUID childId = UUID.randomUUID();
        BankAccount current = new BankAccount(UUID.randomUUID(),
                p.getOrganizationId(),
                childId,
                BankAccount.AccountType.CURRENT,
                1000,
                "USD",
                "C-002",
                true,
                Instant.now(),
                Instant.now());
        BankAccount savings = new BankAccount(UUID.randomUUID(),
                p.getOrganizationId(),
                childId,
                BankAccount.AccountType.SAVINGS,
                0,
                "USD",
                "S-002",
                true,
                Instant.now(),
                Instant.now());
        accountRepo.save(current);
        accountRepo.save(savings);

        assertThrows(SecurityException.class,
                () -> bankService.transferToSavings(parentId, "wrong", childId, 500));

        assertThrows(IllegalStateException.class,
                () -> bankService.transferToSavings(parentId, parentPin, childId, 2000));
    }

    @Test
    void depositToCurrent_works() {
        UUID childId = UUID.randomUUID();
        UUID org = UUID.randomUUID();
        BankAccount current = new BankAccount(UUID.randomUUID(),
                org,
                childId,
                BankAccount.AccountType.CURRENT,
                0,
                "USD",
                "C-003",
                true,
                Instant.now(),
                Instant.now());
        accountRepo.save(current);

        bankService.depositToCurrent(childId, 250);
        BankAccount updated = accountRepo.findById(current.getId()).orElseThrow();
        assertEquals(250, updated.getBalance());
    }
}
