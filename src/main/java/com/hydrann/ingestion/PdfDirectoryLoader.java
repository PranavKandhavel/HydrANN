package com.hydrann.ingestion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class PdfDirectoryLoader {

    /**
     * Finds all PDF files inside the given directory.
     *
     * The search is recursive, so PDFs inside subdirectories
     * will also be discovered.
     *
     * Example:
     *
     * data/raw-docs/
     *     java.pdf
     *     networking.pdf
     *     college/
     *         dbms.pdf
     *
     * All three PDFs will be returned.
     */
    public List<Path> load(Path directory) throws IOException {

        if (directory == null) {
            throw new IllegalArgumentException(
                    "PDF directory cannot be null"
            );
        }

        if (!Files.exists(directory)) {
            throw new IOException(
                    "PDF directory does not exist: " + directory
            );
        }

        if (!Files.isDirectory(directory)) {
            throw new IOException(
                    "Path is not a directory: " + directory
            );
        }

        List<Path> pdfFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(directory)) {

            paths.filter(Files::isRegularFile)
                    .filter(this::isPdf)
                    .forEach(pdfFiles::add);
        }

        // Make processing order deterministic.
        pdfFiles.sort(Comparator.comparing(
                path -> path.toAbsolutePath().toString()
        ));

        return pdfFiles;
    }

    private boolean isPdf(Path path) {

        String fileName =
                path.getFileName().toString();

        return fileName
                .toLowerCase()
                .endsWith(".pdf");
    }
}