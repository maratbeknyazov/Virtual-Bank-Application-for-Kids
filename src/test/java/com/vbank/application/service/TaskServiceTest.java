package com.vbank.application.service;

import com.vbank.domain.model.BankAccount;
import com.vbank.domain.model.Task;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {
    private Path dataDir;
    private JsonRepository<Task> taskRepo;
    private JsonRepository<BankAccount> accountRepo;
    private JsonRepository<User> userRepo;
    private BankService bankService;
    private TaskService taskService;

    @BeforeEach
    void setUp() throws IOException {
        dataDir = Path.of("data");
        if (Files.exists(dataDir)) {
            deleteRecursively(dataDir);
        }
        taskRepo = new JsonRepository<>(Task.class);
        accountRepo = new JsonRepository<>(BankAccount.class);
        userRepo = new JsonRepository<>(User.class);
        bankService = new BankService(accountRepo, userRepo);
        taskService = new TaskService(taskRepo, bankService);
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
    void approveTask_happyPath() {
        UUID parentId = UUID.randomUUID();
        String pin = "4321";
        String hash = BCrypt.hashpw(pin, BCrypt.gensalt());
        User parent = new User(parentId,
                UUID.randomUUID(),
                "mom",
                "Mom",
                User.Role.PARENT,
                hash,
                null,
                false,
                Instant.now(),
                Instant.now());
        userRepo.save(parent);

        UUID childId = UUID.randomUUID();
        BankAccount current = new BankAccount(UUID.randomUUID(),
                parent.getOrganizationId(),
                childId,
                BankAccount.AccountType.CURRENT,
                0,
                "USD",
                "C-100",
                true,
                Instant.now(),
                Instant.now());
        accountRepo.save(current);

        Task task = new Task(UUID.randomUUID(),
                parent.getOrganizationId(),
                parentId,
                childId,
                "Clean your room",
                500,
                Task.Status.SUBMITTED,
                "chores",
                LocalDate.now().plusDays(3),
                Instant.now(),
                null,
                Instant.now(),
                Instant.now());
        taskRepo.save(task);

        taskService.approveTask(task.getId(), parentId, pin);

        Task saved = taskRepo.findById(task.getId()).orElseThrow();
        assertEquals(Task.Status.COMPLETED, saved.getTaskStatus());

        BankAccount updated = accountRepo.findById(current.getId()).orElseThrow();
        assertEquals(500, updated.getBalance());
    }

    @Test
    void approveTask_failsOnWrongParentOrPin() {
        UUID parentId = UUID.randomUUID();
        String pin = "1111";
        String hash = BCrypt.hashpw(pin, BCrypt.gensalt());
        User parent = new User(parentId,
                UUID.randomUUID(),
                "dad",
                "Dad",
                User.Role.PARENT,
                hash,
                null,
                false,
                Instant.now(),
                Instant.now());
        userRepo.save(parent);

        UUID childId = UUID.randomUUID();
        BankAccount current = new BankAccount(UUID.randomUUID(),
                parent.getOrganizationId(),
                childId,
                BankAccount.AccountType.CURRENT,
                0,
                "USD",
                "C-200",
                true,
                Instant.now(),
                Instant.now());
        accountRepo.save(current);

        Task task = new Task(UUID.randomUUID(),
                parent.getOrganizationId(),
                parentId,
                childId,
                "Do homework",
                300,
                Task.Status.SUBMITTED,
                "school",
                LocalDate.now().plusDays(1),
                Instant.now(),
                null,
                Instant.now(),
                Instant.now());
        taskRepo.save(task);

        assertThrows(SecurityException.class,
                () -> taskService.approveTask(task.getId(), UUID.randomUUID(), pin));

        assertThrows(SecurityException.class,
                () -> taskService.approveTask(task.getId(), parentId, "bad"));
    }
}
