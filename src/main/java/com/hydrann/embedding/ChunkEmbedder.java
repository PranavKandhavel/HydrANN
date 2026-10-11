package com.hydrann.embedding;

import com.hydrann.ingestion.ChunkEmbedding;
import com.hydrann.ingestion.TextChunk;

import java.util.ArrayList;
import java.util.List;

public class ChunkEmbedder {

    private final MiniLMEmbedder embedder;

    public ChunkEmbedder(MiniLMEmbedder embedder) {
        this.embedder = embedder;
    }

    /**
     * Converts semantic text chunks into embedded chunks.
     *
     * Each TextChunk keeps its original metadata:
     * - documentId
     * - pageNumber
     * - chunkIndex
     * - text
     *
     * The chunk text is passed to MiniLM and the resulting
     * normalized 384-dimensional vector is attached to it.
     */
    public List<ChunkEmbedding> embedChunks(
            List<TextChunk> chunks) throws Exception {

        List<ChunkEmbedding> embeddedChunks =
                new ArrayList<>();

        for (TextChunk chunk : chunks) {

            float[] embedding =
                    embedder.embed(chunk.getText());

            ChunkEmbedding chunkEmbedding =
                    new ChunkEmbedding(
                            chunk,
                            embedding
                    );

            embeddedChunks.add(chunkEmbedding);
        }

        return embeddedChunks;
    }
}