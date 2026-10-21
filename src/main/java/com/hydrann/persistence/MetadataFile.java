package com.hydrann.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class MetadataFile {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<VectorMetadata> load(Path path) throws IOException {
        List<VectorMetadata> records = new ArrayList<>();

        if (!Files.exists(path)) {
            return records;
        }

        List<String> lines = Files.readAllLines(
                path, StandardCharsets.UTF_8
        );

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.isBlank()) {
                throw new IOException(
                        "Blank metadata line at line " + (i + 1)
                );
            }

            try {
                VectorMetadata record =
                        mapper.readValue(line, VectorMetadata.class);

                if (record.vectorId() != i) {
                    throw new IOException(
                            "Metadata ID mismatch at line " + (i + 1)
                    );
                }

                records.add(record);
            } catch (IOException e) {
                throw new IOException(
                        "Invalid metadata at line " + (i + 1), e
                );
            }
        }

        return records;
    }

    public void save(
            Path path,
            List<VectorMetadata> records) throws IOException {

        Path absolutePath = path.toAbsolutePath();
        Files.createDirectories(absolutePath.getParent());

        Path tempPath = absolutePath.resolveSibling(
                absolutePath.getFileName() + ".tmp"
        );

        List<String> lines = new ArrayList<>(records.size());

        for (int i = 0; i < records.size(); i++) {
            VectorMetadata record = records.get(i);

            if (record.vectorId() != i) {
                throw new IOException(
                        "Metadata IDs must be contiguous and ordered"
                );
            }

            lines.add(mapper.writeValueAsString(record));
        }

        Files.write(
                tempPath,
                lines,
                StandardCharsets.UTF_8
        );

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