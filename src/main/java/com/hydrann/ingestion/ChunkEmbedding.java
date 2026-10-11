package com.hydrann.ingestion;

public class ChunkEmbedding {

    private final TextChunk chunk;
    private final float[] embedding;

    public ChunkEmbedding(
            TextChunk chunk,
            float[] embedding) {

        this.chunk = chunk;
        this.embedding = embedding;
    }

    public TextChunk getChunk() {
        return chunk;
    }

    public float[] getEmbedding() {
        return embedding;
    }
}