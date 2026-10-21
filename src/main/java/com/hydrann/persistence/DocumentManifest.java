package com.hydrann.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class DocumentManifest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path path;
    private Map<String, String> fingerprints = new HashMap<>();

    public DocumentManifest(Path path) throws IOException {
        this.path = path;
        load();
    }

    private void load() throws IOException {
        if (!Files.exists(path)) {
            fingerprints = new HashMap<>();
            return;
        }

        fingerprints = mapper.readValue(
                path.toFile(),
                new TypeReference<Map<String, String>>() {}
        );
    }

    public boolean isUnchanged(String documentPath, String fingerprint) {
        return fingerprint.equals(fingerprints.get(documentPath));
    }

    public void markProcessed(
            String documentPath,
            String fingerprint) throws IOException {

        Map<String, String> updated = new HashMap<>(fingerprints);
        updated.put(documentPath, fingerprint);
        save(updated);
        fingerprints = updated;
    }

    public void removeMissingDocuments(
            java.util.Set<String> existingPaths) throws IOException {

        Map<String, String> updated = new HashMap<>(fingerprints);
        updated.keySet().retainAll(existingPaths);
        save(updated);
        fingerprints = updated;
    }

    public java.util.Set<String> getKnownPaths() {
        return java.util.Set.copyOf(fingerprints.keySet());
    }

    private void save(Map<String, String> updated) throws IOException {
        Path absolutePath = path.toAbsolutePath();
        Files.createDirectories(absolutePath.getParent());

        Path tempPath = absolutePath.resolveSibling(
                absolutePath.getFileName() + ".tmp"
        );

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(tempPath.toFile(), updated);

        try {
            Files.move(
                    tempPath,
                    absolutePath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(
                    tempPath,
                    absolutePath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }
}