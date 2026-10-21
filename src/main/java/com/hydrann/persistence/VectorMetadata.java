package com.hydrann.persistence;

public record VectorMetadata(
        long vectorId,
        String documentId,
        String documentPath,
        String fingerprint,
        int pageNumber,
        int chunkIndex,
        String text,
        boolean active
) {}