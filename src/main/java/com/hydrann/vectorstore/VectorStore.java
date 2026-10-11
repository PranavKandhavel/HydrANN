package com.hydrann.vectorstore;

import com.hydrann.ingestion.ChunkEmbedding;

import java.util.List;

public interface VectorStore {

    void add(ChunkEmbedding chunkEmbedding);

    void addAll(List<ChunkEmbedding> chunkEmbeddings);

    List<SearchResult> search(
            float[] queryVector,
            int k
    );

    int size();
}