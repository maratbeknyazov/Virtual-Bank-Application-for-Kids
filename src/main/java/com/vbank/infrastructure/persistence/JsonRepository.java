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
 * Simple JSON-backed repository implementation using Jackson.
 * Data is stored in the {@code data/} directory at project root.
 * Filenames are derived from the entity class name (lowercased + "s.json").
 * <p>
 * The implementation reads and writes the entire list on each operation; this
 * is sufficient for the small scale of the demo application.
 */
public class JsonRepository<T> implements Repository<T> {
    private final Class<T> clazz;
    private final Path filePath;
    private final ObjectMapper mapper;

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
