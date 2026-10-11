package com.hydrann.vectorstore;

import com.hydrann.ingestion.ChunkEmbedding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InMemoryVectorStore implements VectorStore {

    private final List<ChunkEmbedding> entries;

    public InMemoryVectorStore() {
        this.entries = new ArrayList<>();
    }

    @Override
    public void add(ChunkEmbedding chunkEmbedding) {

        if (chunkEmbedding == null) {
            throw new IllegalArgumentException(
                    "Chunk embedding cannot be null"
            );
        }

        validateEmbedding(
                chunkEmbedding.getEmbedding()
        );

        entries.add(chunkEmbedding);
    }

    @Override
    public void addAll(
            List<ChunkEmbedding> chunkEmbeddings) {

        if (chunkEmbeddings == null) {
            throw new IllegalArgumentException(
                    "Chunk embeddings cannot be null"
            );
        }

        for (ChunkEmbedding chunkEmbedding :
                chunkEmbeddings) {

            add(chunkEmbedding);
        }
    }

    @Override
    public List<SearchResult> search(
            float[] queryVector,
            int k) {

        if (queryVector == null) {
            throw new IllegalArgumentException(
                    "Query vector cannot be null"
            );
        }

        if (k <= 0) {
            throw new IllegalArgumentException(
                    "k must be greater than zero"
            );
        }

        validateEmbedding(queryVector);

        List<SearchResult> results =
                new ArrayList<>();

        /*
         * Compare the query against every stored vector.
         *
         * Because all MiniLM vectors are L2-normalized,
         * cosine similarity is simply the dot product.
         */
        for (ChunkEmbedding entry : entries) {

            float[] storedVector =
                    entry.getEmbedding();

            double similarity =
                    dotProduct(
                            queryVector,
                            storedVector
                    );

            results.add(
                    new SearchResult(
                            entry,
                            similarity
                    )
            );
        }

        /*
         * Highest similarity = most relevant.
         */
        results.sort(
                Comparator.comparingDouble(
                        SearchResult::getScore
                ).reversed()
        );

        /*
         * Return at most k results.
         */
        int resultCount =
                Math.min(k, results.size());

        return new ArrayList<>(
                results.subList(0, resultCount)
        );
    }

    @Override
    public int size() {
        return entries.size();
    }

    /*
     * Dot product:
     *
     * A · B =
     * A[0]B[0] + A[1]B[1] + ... + A[n]B[n]
     *
     * Since both vectors are normalized:
     *
     * cosine(A, B) = A · B
     */
    private double dotProduct(
            float[] a,
            float[] b) {

        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Vector dimensions do not match: " +
                    a.length + " vs " + b.length
            );
        }

        double sum = 0.0;

        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }

        return sum;
    }

    private void validateEmbedding(
            float[] embedding) {

        if (embedding == null) {
            throw new IllegalArgumentException(
                    "Embedding cannot be null"
            );
        }

        if (embedding.length == 0) {
            throw new IllegalArgumentException(
                    "Embedding cannot be empty"
            );
        }

        for (float value : embedding) {

            if (Float.isNaN(value) ||
                    Float.isInfinite(value)) {

                throw new IllegalArgumentException(
                        "Embedding contains NaN or Infinity"
                );
            }
        }
    }
}