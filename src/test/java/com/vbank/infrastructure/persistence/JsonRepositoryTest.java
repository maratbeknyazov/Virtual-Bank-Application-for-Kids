package com.vbank.infrastructure.persistence;

import com.vbank.domain.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JsonRepositoryTest {
    private Path dataDir;

    @BeforeEach
    void setUp() throws IOException {
        dataDir = Path.of("data");
        if (Files.exists(dataDir)) {
            deleteRecursively(dataDir);
        }
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
    void basicCrudOperations() {
        JsonRepository<User> repo = new JsonRepository<>(User.class);

        UUID id = UUID.randomUUID();
        User user = new User(id,
                UUID.randomUUID(),
                "jdoe",
                "John Doe",
                User.Role.PARENT,
                "hash",
                null,
                false,
                Instant.now(),
                Instant.now());

        // initially empty
        assertTrue(repo.findAll().isEmpty());
        assertTrue(repo.findById(id).isEmpty());

        repo.save(user);
        assertEquals(1, repo.findAll().size());
        assertEquals(user, repo.findById(id).orElseThrow());

        // update
        User user2 = new User(id,
                user.getOrganizationId(),
                "jdoe",
                "John Doe",
                User.Role.PARENT,
                "hash2",
                null,
                false,
                user.getCreatedAt(),
                Instant.now());
        repo.save(user2);
        assertEquals(1, repo.findAll().size());
        assertEquals("hash2", repo.findById(id).orElseThrow().getPinHash());

        repo.delete(id);
        assertTrue(repo.findAll().isEmpty());
    }
}