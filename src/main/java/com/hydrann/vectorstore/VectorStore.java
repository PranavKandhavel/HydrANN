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
    int idLimit();                       // ids are 0..idLimit-1, including deactivated
    boolean isActive(int id);
    float[] vector(int id);              // raw vector only, no object building
    ChunkEmbedding get(int id); 
}