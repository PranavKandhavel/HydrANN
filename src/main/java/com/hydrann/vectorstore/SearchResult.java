package com.hydrann.vectorstore;

import com.hydrann.ingestion.ChunkEmbedding;

public class SearchResult {

    private final ChunkEmbedding chunkEmbedding;
    private final double score;

    public SearchResult(
            ChunkEmbedding chunkEmbedding,
            double score) {

        this.chunkEmbedding = chunkEmbedding;
        this.score = score;
    }

    public ChunkEmbedding getChunkEmbedding() {
        return chunkEmbedding;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {

        return "SearchResult{" +
                "documentId='" +
                chunkEmbedding.getChunk().getDocumentId() +
                '\'' +
                ", pageNumber=" +
                chunkEmbedding.getChunk().getPageNumber() +
                ", chunkIndex=" +
                chunkEmbedding.getChunk().getChunkIndex() +
                ", score=" +
                score +
                '}';
    }
}