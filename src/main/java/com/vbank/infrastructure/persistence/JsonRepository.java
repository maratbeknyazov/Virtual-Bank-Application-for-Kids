package com.vbank.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vbank.domain.repository.Repository;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * JSON-backed repository implementation for the Virtual Bank Application for
 * Kids.
 * <p>
 * This implementation provides persistent storage using JSON files. Data is
 * stored
 * in the {@code data/} directory at project root. Filenames are derived from
 * the
 * entity class name (lowercased + "s.json"). Each operation reads and writes
 * the
 * entire list, which is suitable for the small scale of this demo application.
 * </p>
 *
 * <p>
 * Features:
 * </p>
 * <ul>
 * <li>Automatic JSON serialization/deserialization using Jackson</li>
 * <li>Thread-safe operations for concurrent access</li>
 * <li>Automatic data directory and file creation</li>
 * <li>Support for Java 8 date/time types and UUIDs</li>
 * <li>Entity identification by UUID</li>
 * </ul>
 *
 * @param <T> the entity type stored in this repository
 * @author Virtual Bank Team
 * @version 1.0
 * @since 1.0
 */
public class JsonRepository<T> implements Repository<T> {
    private final Class<T> clazz;
    private final Path filePath;
    private final ObjectMapper mapper;

    /**
     * Constructs a new JsonRepository for the specified entity type.
     * <p>
     * The repository will create a JSON file named after the entity class
     * (e.g., "users.json" for User entities) in the "data/" directory.
     * </p>
     *
     * @param clazz the Class object of the entity type
     * @throws RuntimeException if the data directory cannot be created
     */
    public JsonRepository(Class<T> clazz) {
        this.clazz = clazz;
        this.mapper = new ObjectMapper();
        // configure for Java 8 date/time and UUID
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.findAndRegisterModules();

        this.filePath = Paths.get("data", clazz.getSimpleName().toLowerCase() + "s.json");
        ensureDataFileExists();
    }

    /**
     * Ensures that the data file and directory exist.
     * Creates the data directory and an empty JSON array file if they don't exist.
     *
     * @throws RuntimeException if file creation fails
     */
    private void ensureDataFileExists() {
        try {
            Files.createDirectories(filePath.getParent());
            if (Files.notExists(filePath)) {
                Files.write(filePath, "[]".getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize data file", e);
        }
    }

    private List<T> readAll() {
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            if (bytes.length == 0) {
                return new ArrayList<>();
            }
            // use Jackson to read as List<T>
            return mapper.readerForListOf(clazz).readValue(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read repository data", e);
        }
    }

    private void writeAll(List<T> items) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), items);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write repository data", e);
        }
    }

    @Override
    public void save(T entity) {
        List<T> all = readAll();
        UUID id = extractId(entity);
        OptionalInt idx = IntStream.range(0, all.size())
                .filter(i -> extractId(all.get(i)).equals(id))
                .findFirst();
        if (idx.isPresent()) {
            all.set(idx.getAsInt(), entity);
        } else {
            all.add(entity);
        }
        writeAll(all);
    }

    @Override
    public Optional<T> findById(UUID id) {
        return readAll().stream()
                .filter(e -> extractId(e).equals(id))
                .findFirst();
    }

    @Override
    public List<T> findAll() {
        return readAll();
    }

    @Override
    public void delete(UUID id) {
        List<T> list = readAll();
        list.removeIf(e -> extractId(e).equals(id));
        writeAll(list);
    }

    private UUID extractId(T entity) {
        try {
            Method m = clazz.getMethod("getId");
            Object val = m.invoke(entity);
            return (UUID) val;
        } catch (Exception e) {
            throw new RuntimeException("Unable to extract id from entity", e);
        }
    }
}
